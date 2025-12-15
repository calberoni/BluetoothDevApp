package com.parkingate.controller.viewmodel

import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.parkingate.controller.ble.BleConnectionState
import com.parkingate.controller.ble.BleManager
import com.parkingate.controller.data.UserRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * ViewModel principal que coordina la lógica de negocio de la aplicación.
 *
 * Este ViewModel sigue el patrón MVVM (Model-View-ViewModel):
 * - **Model**: BleManager y UserRepository (lógica de datos y BLE)
 * - **View**: MainScreen (UI con Jetpack Compose)
 * - **ViewModel**: Esta clase (lógica de presentación y coordinación)
 *
 * ## Responsabilidades:
 *
 * 1. **Gestión de estado UI**
 *    - Expone estados observables (StateFlow) para la UI
 *    - Transforma estados de BLE en estados de UI comprensibles
 *
 * 2. **Coordinación de operaciones**
 *    - Orquesta el flujo completo: obtener UUID → escanear → conectar → enviar
 *    - Maneja la secuencia de operaciones BLE
 *
 * 3. **Manejo de permisos**
 *    - Verifica que los permisos BLE estén concedidos
 *    - Proporciona feedback cuando faltan permisos
 *
 * 4. **Ciclo de vida**
 *    - Limpia recursos BLE cuando el ViewModel se destruye
 *    - Cancela operaciones en curso si es necesario
 *
 * ## Flujo de operación cuando el usuario presiona "ABRIR":
 *
 * 1. onOpenButtonClick() es llamado desde la UI
 * 2. Se verifica que no haya operación en curso
 * 3. Se verifica que el Bluetooth esté habilitado
 * 4. Se obtiene el UUID del usuario desde UserRepository
 * 5. Se inicia el escaneo BLE con BleManager
 * 6. BleManager emite estados que se reflejan en connectionState
 * 7. Cuando se conecta, se envía automáticamente el UUID
 * 8. La UI reacciona a los cambios de estado y muestra feedback
 *
 * @property application Contexto de aplicación para acceder a servicios del sistema
 */
class MainViewModel(application: Application) : AndroidViewModel(application) {

    companion object {
        private const val TAG = "MainViewModel"
    }

    private val bleManager = BleManager(application.applicationContext)
    private val userRepository = UserRepository(application.applicationContext)
    private val themePreferences = com.parkingate.controller.data.ThemePreferences(application.applicationContext)
    private val historyRepository = com.parkingate.controller.data.ConnectionHistoryRepository(application.applicationContext)
    private val hapticManager = com.parkingate.controller.util.HapticFeedbackManager(application.applicationContext)
    private val debugManager = com.parkingate.controller.debug.DebugManager(application.applicationContext)

    /**
     * Estado de conexión BLE observable por la UI.
     *
     * Este StateFlow emite cambios cada vez que el estado de la conexión
     * BLE cambia, permitiendo que la UI reaccione automáticamente.
     */
    val connectionState: StateFlow<BleConnectionState> = bleManager.connectionState

    /**
     * UUID del usuario almacenado.
     *
     * Se obtiene de forma reactiva desde el UserRepository.
     */
    private val _userUuid = MutableStateFlow<String?>(null)
    val userUuid: StateFlow<String?> = _userUuid.asStateFlow()

    /**
     * Indica si hay una operación BLE en curso.
     *
     * Útil para deshabilitar el botón mientras se procesa una operación.
     */
    private val _isProcessing = MutableStateFlow(false)
    val isProcessing: StateFlow<Boolean> = _isProcessing.asStateFlow()

    /**
     * Log de operaciones BLE en tiempo real.
     *
     * Mantiene un historial de todos los eventos BLE para visualización
     * en la UI con fines de debugging y desarrollo.
     */
    private val _bleLog = MutableStateFlow<List<String>>(emptyList())
    val bleLog: StateFlow<List<String>> = _bleLog.asStateFlow()

    /**
     * RSSI (señal) del dispositivo BLE.
     */
    val rssi: StateFlow<Int> = bleManager.rssi

    /**
     * Estado del tema oscuro.
     */
    val isDarkTheme: Flow<Boolean> = themePreferences.isDarkTheme

    /**
     * Historial reciente de conexiones (últimos 20 registros).
     */
    val recentHistory: Flow<List<com.parkingate.controller.data.ConnectionHistory>> =
        historyRepository.getRecentHistory(20)

    /**
     * Estadísticas del historial.
     */
    val totalConnections: Flow<Int> = historyRepository.getTotalConnectionCount()
    val successfulConnections: Flow<Int> = historyRepository.getSuccessfulConnectionCount()

    /**
     * Timestamp de inicio de conexión (para calcular duración)
     */
    private var connectionStartTime: Long = 0

    init {
        // Cargar el UUID del usuario al iniciar
        viewModelScope.launch {
            try {
                _userUuid.value = userRepository.getUserUuid()
                Log.d(TAG, "UUID del usuario cargado: ${_userUuid.value}")
            } catch (e: Exception) {
                Log.e(TAG, "Error al cargar UUID", e)
            }
        }

        // Observar cambios en el estado de conexión para gestionar el procesamiento
        viewModelScope.launch {
            connectionState.collect { state ->
                _isProcessing.value = when (state) {
                    is BleConnectionState.Idle,
                    is BleConnectionState.Success,
                    is BleConnectionState.Error -> false
                    else -> true
                }

                // Agregar el cambio de estado al log y feedback háptico
                val logMessage = when (state) {
                    is BleConnectionState.Idle -> "Estado: Idle (Esperando)"
                    is BleConnectionState.Scanning -> {
                        connectionStartTime = System.currentTimeMillis()
                        hapticManager.performProcessing()
                        "Iniciando escaneo BLE..."
                    }
                    is BleConnectionState.Connecting -> {
                        hapticManager.performClick()
                        "Conectando al dispositivo..."
                    }
                    is BleConnectionState.Connected -> {
                        hapticManager.performClick()
                        "✓ Conectado - Servicios descubiertos"
                    }
                    is BleConnectionState.Opening -> {
                        hapticManager.performProcessing()
                        "Enviando comando de apertura..."
                    }
                    is BleConnectionState.Success -> {
                        // Guardar conexión exitosa en el historial
                        saveConnectionToHistory(
                            result = com.parkingate.controller.data.ConnectionResult.SUCCESS,
                            message = "Operación completada exitosamente"
                        )
                        hapticManager.performSuccess()
                        "✓ Operación completada exitosamente"
                    }
                    is BleConnectionState.Error -> {
                        // Guardar error en el historial
                        saveConnectionToHistory(
                            result = com.parkingate.controller.data.ConnectionResult.ERROR,
                            message = state.message
                        )
                        hapticManager.performError()
                        "✗ Error: ${state.message}"
                    }
                }
                addLog(logMessage)

                // Si se conectó exitosamente, enviar el comando de apertura
                if (state is BleConnectionState.Connected) {
                    sendOpenCommandToDevice()
                }
            }
        }

        // Observar eventos del BleManager
        viewModelScope.launch {
            bleManager.bleEvents.collect { event ->
                event?.let { addLog(it) }
            }
        }
    }

    /**
     * Maneja el evento de presionar el botón "ABRIR".
     *
     * Este método orquesta todo el flujo de apertura:
     * 1. Verifica que no haya operación en curso
     * 2. Verifica soporte BLE
     * 3. Verifica que Bluetooth esté habilitado
     * 4. Inicia el escaneo de dispositivos
     *
     * La UI debe mostrar estados según los eventos emitidos en connectionState.
     */
    fun onOpenButtonClick() {
        // Evitar múltiples presiones
        if (_isProcessing.value) {
            Log.d(TAG, "Operación ya en curso, ignorando clic")
            return
        }

        // Verificar soporte BLE
        if (!bleManager.isBluetoothSupported()) {
            Log.e(TAG, "Dispositivo no soporta BLE")
            return
        }

        // Verificar que Bluetooth esté habilitado
        if (!bleManager.isBluetoothEnabled()) {
            Log.e(TAG, "Bluetooth desactivado")
            return
        }

        // Iniciar escaneo
        Log.d(TAG, "Iniciando proceso de apertura...")
        bleManager.startScanning()
    }

    /**
     * Envía el comando de apertura al dispositivo ESP32.
     *
     * Esta función se llama automáticamente cuando el estado cambia a Connected.
     * Obtiene el UUID del usuario y lo envía al ESP32 vía BLE.
     */
    private fun sendOpenCommandToDevice() {
        viewModelScope.launch {
            try {
                val uuid = _userUuid.value
                if (uuid == null) {
                    Log.e(TAG, "UUID del usuario no disponible")
                    addLog("✗ Error: UUID no disponible")
                    return@launch
                }

                Log.d(TAG, "Enviando comando de apertura con UUID: $uuid")
                addLog("Enviando UUID: ${uuid.substring(0, 8)}...")
                bleManager.sendOpenCommand(uuid)
            } catch (e: Exception) {
                Log.e(TAG, "Error al enviar comando de apertura", e)
                addLog("✗ Error al enviar comando: ${e.message}")
            }
        }
    }

    /**
     * Agrega un mensaje al log BLE.
     *
     * @param message Mensaje a agregar
     */
    private fun addLog(message: String) {
        val timestamp = java.text.SimpleDateFormat("HH:mm:ss.SSS", java.util.Locale.getDefault())
            .format(java.util.Date())
        val logEntry = "[$timestamp] $message"
        _bleLog.value = _bleLog.value + logEntry
        Log.d(TAG, logEntry)
    }

    /**
     * Reinicia el estado de conexión a Idle.
     *
     * Útil para limpiar el estado después de un error y permitir
     * que el usuario reintente la operación.
     */
    fun resetState() {
        bleManager.resetState()
        _bleLog.value = emptyList()  // Limpiar logs al reiniciar
    }

    /**
     * Alterna entre tema oscuro y claro.
     */
    fun toggleTheme() {
        viewModelScope.launch {
            themePreferences.toggleTheme()
        }
    }

    /**
     * Establece un UUID personalizado.
     *
     * @param uuid UUID personalizado a establecer
     * @return true si se guardó correctamente, false si el formato es inválido
     */
    suspend fun setCustomUuid(uuid: String): Boolean {
        return try {
            // Validar formato (puede ser UUID o cualquier string)
            if (uuid.isBlank()) {
                Log.w(TAG, "UUID vacío")
                return false
            }

            userRepository.setCustomUuid(uuid.trim())
            _userUuid.value = uuid.trim()
            Log.d(TAG, "UUID personalizado establecido")
            true
        } catch (e: Exception) {
            Log.e(TAG, "Error al establecer UUID personalizado", e)
            false
        }
    }

    /**
     * Resetea el UUID al generado automáticamente.
     */
    suspend fun resetUuid() {
        try {
            userRepository.clearUuid()
            _userUuid.value = userRepository.getUserUuid()
            Log.d(TAG, "UUID reseteado")
        } catch (e: Exception) {
            Log.e(TAG, "Error al resetear UUID", e)
        }
    }

    /**
     * Exporta los logs BLE a un archivo.
     *
     * @param format Formato de exportación (JSON o TXT)
     * @return Intent para compartir el archivo
     */
    suspend fun exportAndShareLogs(
        format: com.parkingate.controller.debug.DebugManager.ExportFormat =
            com.parkingate.controller.debug.DebugManager.ExportFormat.JSON
    ): android.content.Intent? {
        return try {
            val logs = _bleLog.value
            if (logs.isEmpty()) {
                Log.w(TAG, "No hay logs para exportar")
                return null
            }

            val file = debugManager.exportLogsToFile(logs, format, includeSystemInfo = true)
            debugManager.shareLogs(file)
        } catch (e: Exception) {
            Log.e(TAG, "Error al exportar logs", e)
            null
        }
    }

    /**
     * Obtiene información del sistema.
     *
     * @return SystemInfo con detalles del dispositivo y app
     */
    fun getSystemInfo(): com.parkingate.controller.debug.DebugManager.SystemInfo {
        return debugManager.getSystemInfo()
    }

    /**
     * Guarda un registro de conexión en el historial.
     *
     * @param result Resultado de la operación (SUCCESS o ERROR)
     * @param message Mensaje descriptivo
     */
    private fun saveConnectionToHistory(
        result: com.parkingate.controller.data.ConnectionResult,
        message: String
    ) {
        viewModelScope.launch {
            try {
                val duration = System.currentTimeMillis() - connectionStartTime
                val currentRssi = rssi.value
                val currentUuid = _userUuid.value ?: "Unknown"

                // Obtener información del dispositivo BLE (si está disponible)
                // Por ahora usamos valores por defecto ya que no tenemos acceso directo
                // al BluetoothDevice en el ViewModel
                val deviceName = "ESP32_Gate" // Podríamos exponerlo desde BleManager
                val deviceAddress = "Unknown" // Podríamos exponerlo desde BleManager

                historyRepository.addConnection(
                    deviceName = deviceName,
                    deviceAddress = deviceAddress,
                    rssi = currentRssi,
                    result = result,
                    message = message,
                    uuid = currentUuid,
                    duration = duration
                )

                Log.d(TAG, "Conexión guardada en el historial: $result - $message")
            } catch (e: Exception) {
                Log.e(TAG, "Error al guardar en el historial", e)
            }
        }
    }

    /**
     * Limpia recursos cuando el ViewModel es destruido.
     *
     * Es importante desconectar el BLE y liberar recursos para
     * evitar memory leaks y mantener el estado consistente.
     */
    override fun onCleared() {
        super.onCleared()
        bleManager.disconnect()
        Log.d(TAG, "ViewModel limpiado, recursos BLE liberados")
    }

    /**
     * Proporciona un mensaje legible para cada estado de conexión.
     *
     * @param state Estado de conexión BLE actual
     * @return Mensaje descriptivo para mostrar al usuario
     */
    fun getStateMessage(state: BleConnectionState): String {
        return when (state) {
            is BleConnectionState.Idle -> "Listo para abrir"
            is BleConnectionState.Scanning -> "Escaneando dispositivo..."
            is BleConnectionState.Connecting -> "Conectando..."
            is BleConnectionState.Connected -> "Conectado"
            is BleConnectionState.Opening -> "Abriendo pluma..."
            is BleConnectionState.Success -> "¡Listo!"
            is BleConnectionState.Error -> state.message
        }
    }

    /**
     * Verifica si el estado actual permite reintentar la operación.
     *
     * @param state Estado de conexión BLE actual
     * @return true si se puede reintentar, false en caso contrario
     */
    fun canRetry(state: BleConnectionState): Boolean {
        return state is BleConnectionState.Error && state.canRetry
    }
}
