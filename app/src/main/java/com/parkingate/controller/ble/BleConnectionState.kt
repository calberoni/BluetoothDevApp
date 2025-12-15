package com.parkingate.controller.ble

/**
 * Estados posibles de la conexión BLE.
 *
 * Esta sealed class representa todos los estados posibles durante el proceso
 * de conexión y comunicación BLE con el ESP32.
 *
 * Flujo típico de estados:
 * Idle → Scanning → Connecting → Connected → Opening → Success
 *
 * En caso de error, cualquier estado puede transicionar a Error.
 */
sealed class BleConnectionState {
    /**
     * Estado inicial, esperando acción del usuario.
     */
    data object Idle : BleConnectionState()

    /**
     * Escaneando dispositivos BLE cercanos.
     */
    data object Scanning : BleConnectionState()

    /**
     * Intentando conectar con el dispositivo ESP32.
     */
    data object Connecting : BleConnectionState()

    /**
     * Conexión establecida, listo para comunicación.
     */
    data object Connected : BleConnectionState()

    /**
     * Enviando comando de apertura al ESP32.
     */
    data object Opening : BleConnectionState()

    /**
     * Operación completada exitosamente.
     */
    data object Success : BleConnectionState()

    /**
     * Error durante el proceso.
     *
     * @property message Mensaje descriptivo del error.
     * @property canRetry Indica si el usuario puede reintentar la operación.
     */
    data class Error(
        val message: String,
        val canRetry: Boolean = true
    ) : BleConnectionState()
}
