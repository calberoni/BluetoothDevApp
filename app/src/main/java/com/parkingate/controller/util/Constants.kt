package com.parkingate.controller.util

import java.util.UUID

/**
 * Constantes de configuración para la aplicación de control de estacionamiento.
 *
 * Esta clase contiene todos los UUIDs y configuraciones necesarias para la
 * comunicación BLE con el ESP32.
 */
object Constants {
    /**
     * UUID del servicio BLE del ESP32.
     *
     * Este UUID debe estar anunciado por el ESP32 para que la app pueda
     * detectarlo durante el escaneo BLE.
     */
    val SERVICE_UUID: UUID = UUID.fromString("0000abcd-0000-1000-8000-00805f9b34fb")

    /**
     * UUID de la característica BLE para escribir el identificador del usuario.
     *
     * Esta característica debe soportar la operación WRITE en el ESP32.
     * La app enviará el UUID del usuario a esta característica.
     */
    val CHARACTERISTIC_UUID: UUID = UUID.fromString("0000dcba-0000-1000-8000-00805f9b34fb")

    /**
     * Nombre del dispositivo ESP32 (opcional, para filtrado adicional).
     *
     * Puede ser usado para filtrar específicamente por nombre de dispositivo
     * si hay múltiples ESP32 en el área.
     */
    const val DEVICE_NAME = "ParkingGate"

    /**
     * Timeout para el escaneo BLE en milisegundos.
     *
     * Después de este tiempo, el escaneo se detendrá automáticamente
     * si no se encuentra el dispositivo.
     */
    const val SCAN_TIMEOUT_MS = 10000L

    /**
     * Timeout para la conexión BLE en milisegundos.
     *
     * Si la conexión no se establece en este tiempo, se considerará fallida.
     */
    const val CONNECTION_TIMEOUT_MS = 5000L

    /**
     * Timeout para operaciones de escritura BLE en milisegundos.
     */
    const val WRITE_TIMEOUT_MS = 3000L

    /**
     * Clave para almacenar el UUID del usuario en DataStore.
     */
    const val USER_UUID_KEY = "user_uuid"

    /**
     * Nombre del archivo DataStore.
     */
    const val DATASTORE_NAME = "parking_gate_prefs"
}
