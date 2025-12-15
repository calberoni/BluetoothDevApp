package com.parkingate.controller.ble

import android.annotation.SuppressLint
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothGatt
import android.bluetooth.BluetoothGattCallback
import android.bluetooth.BluetoothGattCharacteristic
import android.bluetooth.BluetoothManager
import android.bluetooth.BluetoothProfile
import android.bluetooth.le.BluetoothLeScanner
import android.bluetooth.le.ScanCallback
import android.bluetooth.le.ScanFilter
import android.bluetooth.le.ScanResult
import android.bluetooth.le.ScanSettings
import android.content.Context
import android.os.Build
import android.os.ParcelUuid
import android.util.Log
import com.parkingate.controller.util.Constants
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * Gestor principal de comunicación BLE con el ESP32.
 *
 * Esta clase encapsula toda la lógica de Bluetooth Low Energy:
 * - Escaneo de dispositivos
 * - Conexión al ESP32
 * - Descubrimiento de servicios
 * - Escritura de datos (UUID del usuario)
 * - Manejo de reconexión
 *
 * ## Flujo de operación BLE:
 *
 * 1. **Escaneo (Scanning)**
 *    - Se inicia BluetoothLeScanner con filtros para el SERVICE_UUID
 *    - ScanSettings configurado en SCAN_MODE_LOW_LATENCY para máxima velocidad
 *    - Timeout de 10 segundos
 *
 * 2. **Conexión (Connecting)**
 *    - Cuando se detecta el dispositivo, se llama a connectGatt()
 *    - Se establece la conexión BLE con el ESP32
 *    - BluetoothGattCallback maneja los eventos de conexión
 *
 * 3. **Descubrimiento de servicios (Connected)**
 *    - Después de conectar, se llama a discoverServices()
 *    - Se busca el servicio con SERVICE_UUID
 *    - Se obtiene la característica con CHARACTERISTIC_UUID
 *
 * 4. **Escritura (Opening)**
 *    - Se escribe el UUID del usuario en la característica
 *    - writeCharacteristic() envía los datos al ESP32
 *    - Se espera confirmación en onCharacteristicWrite()
 *
 * 5. **Finalización (Success)**
 *    - Operación completada exitosamente
 *    - Conexión se mantiene para futuras operaciones
 *
 * ## APIs de Android BLE utilizadas:
 *
 * - **BluetoothAdapter**: Punto de entrada principal para operaciones Bluetooth
 * - **BluetoothLeScanner**: Escaneo de dispositivos BLE cercanos
 * - **ScanSettings**: Configuración del escaneo (modo, latencia, etc.)
 * - **ScanFilter**: Filtros para escaneo dirigido (UUID de servicio)
 * - **BluetoothGatt**: Interfaz para comunicación GATT con dispositivo remoto
 * - **BluetoothGattCallback**: Callbacks para eventos GATT (conexión, servicios, escritura)
 * - **BluetoothGattCharacteristic**: Representa una característica GATT para lectura/escritura
 *
 * @property context Contexto de la aplicación para acceder al BluetoothManager
 */
@SuppressLint("MissingPermission")
class BleManager(private val context: Context) {

    companion object {
        private const val TAG = "BleManager"
        private const val MAX_RECONNECT_ATTEMPTS = 3
        private const val INITIAL_RECONNECT_DELAY = 1000L // 1 segundo
        private const val MAX_RECONNECT_DELAY = 8000L // 8 segundos
    }

    private val bluetoothManager: BluetoothManager =
        context.getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager

    private val bluetoothAdapter: BluetoothAdapter? = bluetoothManager.adapter

    private val bluetoothLeScanner: BluetoothLeScanner? = bluetoothAdapter?.bluetoothLeScanner

    private var bluetoothGatt: BluetoothGatt? = null
    private var lastConnectedDevice: BluetoothDevice? = null
    private var reconnectAttempts = 0
    private var autoReconnectEnabled = true

    private val scope = CoroutineScope(Dispatchers.IO)

    private val _connectionState = MutableStateFlow<BleConnectionState>(BleConnectionState.Idle)
    val connectionState: StateFlow<BleConnectionState> = _connectionState.asStateFlow()

    private val _bleEvents = MutableStateFlow<String?>(null)
    val bleEvents: StateFlow<String?> = _bleEvents.asStateFlow()

    private val _rssi = MutableStateFlow<Int>(-100)
    val rssi: StateFlow<Int> = _rssi.asStateFlow()

    private fun emitEvent(message: String) {
        _bleEvents.value = message
    }

    /**
     * Habilita o deshabilita la reconexión automática.
     */
    fun setAutoReconnect(enabled: Boolean) {
        autoReconnectEnabled = enabled
        Log.d(TAG, "Auto-reconexión ${if (enabled) "habilitada" else "deshabilitada"}")
    }

    /**
     * Verifica si el dispositivo soporta BLE.
     */
    fun isBluetoothSupported(): Boolean {
        return bluetoothAdapter != null && context.packageManager.hasSystemFeature(
            android.content.pm.PackageManager.FEATURE_BLUETOOTH_LE
        )
    }

    /**
     * Verifica si el Bluetooth está habilitado.
     */
    fun isBluetoothEnabled(): Boolean {
        return bluetoothAdapter?.isEnabled == true
    }

    /**
     * Callback para el escaneo BLE.
     *
     * Este callback se invoca cuando:
     * - Se detecta un dispositivo BLE que coincide con los filtros
     * - Ocurre un error durante el escaneo
     * - El escaneo se completa en lotes (batch)
     */
    private val scanCallback = object : ScanCallback() {
        override fun onScanResult(callbackType: Int, result: ScanResult?) {
            result?.device?.let { device ->
                Log.d(TAG, "Dispositivo encontrado: ${device.name} - ${device.address}")
                emitEvent("✓ Dispositivo encontrado: ${device.name} (${device.address})")
                stopScanning()
                connectToDevice(device)
            }
        }

        override fun onBatchScanResults(results: MutableList<ScanResult>?) {
            results?.firstOrNull()?.device?.let { device ->
                Log.d(TAG, "Dispositivo encontrado (batch): ${device.name} - ${device.address}")
                stopScanning()
                connectToDevice(device)
            }
        }

        override fun onScanFailed(errorCode: Int) {
            Log.e(TAG, "Error en escaneo BLE: $errorCode")
            _connectionState.value = BleConnectionState.Error(
                message = "Error al escanear dispositivos BLE",
                canRetry = true
            )
        }
    }

    /**
     * Callback GATT para manejar eventos de conexión y comunicación.
     *
     * Este callback maneja:
     * - Cambios de estado de conexión (conectado/desconectado)
     * - Descubrimiento de servicios
     * - Confirmación de escritura de características
     * - Lectura de características (si es necesario)
     */
    private val gattCallback = object : BluetoothGattCallback() {
        override fun onConnectionStateChange(gatt: BluetoothGatt?, status: Int, newState: Int) {
            when (newState) {
                BluetoothProfile.STATE_CONNECTED -> {
                    Log.d(TAG, "Conectado al GATT server, descubriendo servicios...")
                    reconnectAttempts = 0  // Reset contador de reintentos
                    lastConnectedDevice = gatt?.device  // Guardar dispositivo
                    // NO emitir Connected aquí, esperar a onServicesDiscovered
                    gatt?.discoverServices()
                    // Leer RSSI inicial
                    gatt?.readRemoteRssi()
                }
                BluetoothProfile.STATE_DISCONNECTED -> {
                    Log.d(TAG, "Desconectado del GATT server")
                    gatt?.close()
                    bluetoothGatt = null

                    // Intentar reconexión automática si está habilitada
                    if (autoReconnectEnabled &&
                        lastConnectedDevice != null &&
                        reconnectAttempts < MAX_RECONNECT_ATTEMPTS &&
                        _connectionState.value !is BleConnectionState.Success) {

                        reconnectAttempts++
                        val delay = minOf(
                            INITIAL_RECONNECT_DELAY * (1 shl (reconnectAttempts - 1)),
                            MAX_RECONNECT_DELAY
                        )

                        Log.d(TAG, "Intento de reconexión $reconnectAttempts/$MAX_RECONNECT_ATTEMPTS en ${delay}ms")
                        emitEvent("Reconectando... (intento $reconnectAttempts/$MAX_RECONNECT_ATTEMPTS)")

                        scope.launch {
                            delay(delay)
                            lastConnectedDevice?.let { device ->
                                connectToDevice(device)
                            }
                        }
                    } else {
                        if (_connectionState.value !is BleConnectionState.Success) {
                            val message = if (reconnectAttempts >= MAX_RECONNECT_ATTEMPTS) {
                                "Conexión perdida (max reintentos alcanzados)"
                            } else {
                                "Conexión perdida"
                            }
                            _connectionState.value = BleConnectionState.Error(
                                message = message,
                                canRetry = true
                            )
                        }
                    }
                }
            }
        }

        override fun onReadRemoteRssi(gatt: BluetoothGatt?, rssi: Int, status: Int) {
            if (status == BluetoothGatt.GATT_SUCCESS) {
                _rssi.value = rssi
                Log.d(TAG, "RSSI actualizado: $rssi dBm")

                // Leer RSSI periódicamente cada 2 segundos
                scope.launch {
                    delay(2000)
                    if (_connectionState.value is BleConnectionState.Connected ||
                        _connectionState.value is BleConnectionState.Opening) {
                        gatt?.readRemoteRssi()
                    }
                }
            }
        }

        override fun onServicesDiscovered(gatt: BluetoothGatt?, status: Int) {
            if (status == BluetoothGatt.GATT_SUCCESS) {
                Log.d(TAG, "Servicios descubiertos")
                val service = gatt?.getService(Constants.SERVICE_UUID)
                if (service != null) {
                    Log.d(TAG, "Servicio del ESP32 encontrado")
                    // Ahora sí, emitir Connected porque todo está listo
                    _connectionState.value = BleConnectionState.Connected
                } else {
                    Log.e(TAG, "Servicio del ESP32 no encontrado")
                    _connectionState.value = BleConnectionState.Error(
                        message = "Servicio BLE no encontrado en el dispositivo",
                        canRetry = false
                    )
                }
            } else {
                Log.e(TAG, "Error al descubrir servicios: $status")
                _connectionState.value = BleConnectionState.Error(
                    message = "Error al descubrir servicios",
                    canRetry = true
                )
            }
        }

        override fun onCharacteristicWrite(
            gatt: BluetoothGatt?,
            characteristic: BluetoothGattCharacteristic?,
            status: Int
        ) {
            if (status == BluetoothGatt.GATT_SUCCESS) {
                Log.d(TAG, "UUID del usuario enviado exitosamente")
                emitEvent("✓ UUID enviado correctamente al ESP32")
                emitEvent("✓ Comando completado - Desconectando...")
                _connectionState.value = BleConnectionState.Success

                // Desconectar automáticamente después del éxito
                scope.launch {
                    delay(1500) // Breve pausa para que el ESP32 procese
                    Log.d(TAG, "Desconectando automáticamente tras éxito")

                    // Deshabilitar reconexión antes de desconectar
                    // (esto es una desconexión intencional, no un error)
                    autoReconnectEnabled = false
                    disconnect()

                    delay(500)
                    // Reactivar reconexión para futuras conexiones
                    autoReconnectEnabled = true

                    if (_connectionState.value is BleConnectionState.Success) {
                        _connectionState.value = BleConnectionState.Idle
                    }
                }
            } else {
                Log.e(TAG, "Error al escribir característica: $status")
                emitEvent("✗ Error al escribir característica (status: $status)")
                _connectionState.value = BleConnectionState.Error(
                    message = "Error al enviar datos",
                    canRetry = true
                )
            }
        }
    }

    /**
     * Inicia el escaneo de dispositivos BLE.
     *
     * Configuración del escaneo:
     * - SCAN_MODE_LOW_LATENCY: Máxima velocidad, mayor consumo de batería
     * - Filtro por SERVICE_UUID para detectar solo el ESP32
     * - Timeout automático de SCAN_TIMEOUT_MS
     */
    fun startScanning() {
        if (!isBluetoothSupported()) {
            _connectionState.value = BleConnectionState.Error(
                message = "Dispositivo no soporta BLE",
                canRetry = false
            )
            return
        }

        if (!isBluetoothEnabled()) {
            _connectionState.value = BleConnectionState.Error(
                message = "Bluetooth desactivado",
                canRetry = false
            )
            return
        }

        _connectionState.value = BleConnectionState.Scanning
        Log.d(TAG, "Iniciando escaneo BLE...")

        val scanFilter = ScanFilter.Builder()
            .setServiceUuid(ParcelUuid(Constants.SERVICE_UUID))
            .build()

        val scanSettings = ScanSettings.Builder()
            .setScanMode(ScanSettings.SCAN_MODE_LOW_LATENCY)
            .build()

        bluetoothLeScanner?.startScan(listOf(scanFilter), scanSettings, scanCallback)

        scope.launch {
            delay(Constants.SCAN_TIMEOUT_MS)
            if (_connectionState.value is BleConnectionState.Scanning) {
                stopScanning()
                _connectionState.value = BleConnectionState.Error(
                    message = "Dispositivo no encontrado. ¿Está encendido y cerca?",
                    canRetry = true
                )
            }
        }
    }

    /**
     * Detiene el escaneo BLE.
     */
    private fun stopScanning() {
        bluetoothLeScanner?.stopScan(scanCallback)
        Log.d(TAG, "Escaneo detenido")
    }

    /**
     * Conecta al dispositivo BLE encontrado.
     *
     * @param device Dispositivo BluetoothDevice al que conectarse
     */
    private fun connectToDevice(device: BluetoothDevice) {
        _connectionState.value = BleConnectionState.Connecting
        Log.d(TAG, "Conectando a ${device.address}...")

        bluetoothGatt?.close()
        bluetoothGatt = device.connectGatt(
            context,
            false,
            gattCallback,
            BluetoothDevice.TRANSPORT_LE
        )
    }

    /**
     * Envía el UUID del usuario al ESP32.
     *
     * El formato del paquete enviado es:
     * - String UTF-8 del UUID (36 caracteres)
     * - Ejemplo: "550e8400-e29b-41d4-a716-446655440000"
     *
     * El ESP32 debe:
     * 1. Leer la característica CHARACTERISTIC_UUID
     * 2. Convertir los bytes recibidos a String
     * 3. Validar el formato UUID
     * 4. Verificar el UUID contra su base de datos
     * 5. Activar el motor/relé para abrir la pluma
     *
     * @param userUuid UUID único del usuario
     */
    suspend fun sendOpenCommand(userUuid: String): Boolean = withContext(Dispatchers.IO) {
        val gatt = bluetoothGatt
        if (gatt == null) {
            _connectionState.value = BleConnectionState.Error(
                message = "No conectado al dispositivo",
                canRetry = true
            )
            return@withContext false
        }

        _connectionState.value = BleConnectionState.Opening
        Log.d(TAG, "Enviando comando de apertura con UUID: $userUuid")

        val service = gatt.getService(Constants.SERVICE_UUID)
        val characteristic = service?.getCharacteristic(Constants.CHARACTERISTIC_UUID)

        if (characteristic == null) {
            _connectionState.value = BleConnectionState.Error(
                message = "Característica no encontrada",
                canRetry = false
            )
            return@withContext false
        }

        try {
            val data = userUuid.toByteArray(Charsets.UTF_8)

            Log.d(TAG, "UUID convertido a bytes UTF-8: ${data.size} bytes")
            Log.d(TAG, "Contenido en hex: ${data.joinToString(" ") { "%02x".format(it) }}")

            val success = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                // En Android 13+ (API 33+), writeCharacteristic retorna un código de error
                // 0 = Sin error (éxito)
                // Otros valores = Código de error específico
                val writeResult = gatt.writeCharacteristic(
                    characteristic,
                    data,
                    BluetoothGattCharacteristic.WRITE_TYPE_DEFAULT
                )
                Log.d(TAG, "writeCharacteristic (API 33+) resultado: $writeResult")
                writeResult == 0  // 0 = éxito
            } else {
                // En Android 12 y anteriores, usar API deprecated
                @Suppress("DEPRECATION")
                characteristic.value = data
                @Suppress("DEPRECATION")
                val writeResult = gatt.writeCharacteristic(characteristic)
                Log.d(TAG, "writeCharacteristic (API < 33) resultado: $writeResult")
                writeResult  // Retorna boolean directamente
            }

            if (!success) {
                Log.e(TAG, "writeCharacteristic falló inmediatamente")
                _connectionState.value = BleConnectionState.Error(
                    message = "Error al enviar comando",
                    canRetry = true
                )
            } else {
                Log.d(TAG, "writeCharacteristic iniciado correctamente, esperando callback...")
            }

            return@withContext success
        } catch (e: Exception) {
            Log.e(TAG, "Error al escribir característica", e)
            _connectionState.value = BleConnectionState.Error(
                message = "Error: ${e.message}",
                canRetry = true
            )
            return@withContext false
        }
    }

    /**
     * Desconecta y libera recursos BLE.
     *
     * Debe ser llamado cuando la app se cierra o se detiene.
     */
    fun disconnect() {
        stopScanning()
        bluetoothGatt?.close()
        bluetoothGatt = null
        _connectionState.value = BleConnectionState.Idle
        Log.d(TAG, "Desconectado y recursos liberados")
    }

    /**
     * Reinicia el estado de conexión a Idle.
     */
    fun resetState() {
        _connectionState.value = BleConnectionState.Idle
    }
}
