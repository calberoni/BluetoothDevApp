package com.parkingate.controller.data

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.util.Date

/**
 * Entidad que representa un registro de conexión BLE en el historial.
 *
 * Cada vez que se realiza una conexión exitosa con el dispositivo ESP32,
 * se guarda un registro en la base de datos con la siguiente información:
 * - Timestamp de cuándo ocurrió
 * - Dispositivo conectado (nombre y MAC)
 * - RSSI (intensidad de señal)
 * - Resultado de la operación
 * - UUID enviado
 */
@Entity(tableName = "connection_history")
data class ConnectionHistory(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,

    /**
     * Timestamp de la conexión (milisegundos desde epoch)
     */
    val timestamp: Long = System.currentTimeMillis(),

    /**
     * Nombre del dispositivo BLE conectado
     */
    val deviceName: String?,

    /**
     * Dirección MAC del dispositivo
     */
    val deviceAddress: String,

    /**
     * RSSI (señal) al momento de la conexión
     */
    val rssi: Int,

    /**
     * Resultado de la operación: success, error
     */
    val result: ConnectionResult,

    /**
     * Mensaje descriptivo (opcional)
     */
    val message: String? = null,

    /**
     * UUID enviado al dispositivo
     */
    val uuid: String,

    /**
     * Duración de la conexión en milisegundos
     */
    val duration: Long = 0
) {
    /**
     * Obtiene el timestamp como Date
     */
    fun getDate(): Date = Date(timestamp)
}

/**
 * Resultado de una operación de conexión
 */
enum class ConnectionResult {
    SUCCESS,
    ERROR
}
