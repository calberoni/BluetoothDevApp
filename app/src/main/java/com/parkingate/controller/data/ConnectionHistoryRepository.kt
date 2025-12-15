package com.parkingate.controller.data

import android.content.Context
import kotlinx.coroutines.flow.Flow

/**
 * Repositorio para gestionar el historial de conexiones BLE.
 *
 * Actúa como capa de abstracción entre el ViewModel y la base de datos Room.
 * Proporciona métodos para:
 * - Guardar nuevas conexiones
 * - Consultar el historial
 * - Obtener estadísticas
 *
 * @property context Contexto de la aplicación
 */
class ConnectionHistoryRepository(context: Context) {

    private val dao: ConnectionHistoryDao =
        AppDatabase.getDatabase(context).connectionHistoryDao()

    /**
     * Guarda un nuevo registro de conexión en el historial.
     *
     * @param deviceName Nombre del dispositivo BLE
     * @param deviceAddress Dirección MAC del dispositivo
     * @param rssi Intensidad de señal
     * @param result Resultado de la operación (SUCCESS o ERROR)
     * @param message Mensaje descriptivo (opcional)
     * @param uuid UUID enviado al dispositivo
     * @param duration Duración de la conexión en ms
     * @return ID del registro insertado
     */
    suspend fun addConnection(
        deviceName: String?,
        deviceAddress: String,
        rssi: Int,
        result: ConnectionResult,
        message: String? = null,
        uuid: String,
        duration: Long = 0
    ): Long {
        val history = ConnectionHistory(
            deviceName = deviceName,
            deviceAddress = deviceAddress,
            rssi = rssi,
            result = result,
            message = message,
            uuid = uuid,
            duration = duration
        )
        return dao.insert(history)
    }

    /**
     * Obtiene todo el historial de conexiones.
     *
     * @return Flow que emite la lista completa del historial
     */
    fun getAllHistory(): Flow<List<ConnectionHistory>> = dao.getAllHistory()

    /**
     * Obtiene los registros más recientes.
     *
     * @param limit Número máximo de registros (por defecto 20)
     * @return Flow que emite la lista de registros recientes
     */
    fun getRecentHistory(limit: Int = 20): Flow<List<ConnectionHistory>> =
        dao.getRecentHistory(limit)

    /**
     * Obtiene el historial dentro de un rango de fechas.
     *
     * @param startTime Timestamp inicial
     * @param endTime Timestamp final
     * @return Flow que emite la lista de registros en el rango
     */
    fun getHistoryInRange(startTime: Long, endTime: Long): Flow<List<ConnectionHistory>> =
        dao.getHistoryInRange(startTime, endTime)

    /**
     * Obtiene el conteo total de conexiones.
     *
     * @return Flow que emite el número total de conexiones
     */
    fun getTotalConnectionCount(): Flow<Int> = dao.getTotalConnectionCount()

    /**
     * Obtiene el conteo de conexiones exitosas.
     *
     * @return Flow que emite el número de conexiones exitosas
     */
    fun getSuccessfulConnectionCount(): Flow<Int> = dao.getSuccessfulConnectionCount()

    /**
     * Obtiene el RSSI promedio.
     *
     * @return Flow que emite el RSSI promedio
     */
    fun getAverageRssi(): Flow<Double?> = dao.getAverageRssi()

    /**
     * Obtiene el último registro de conexión.
     *
     * @return Flow que emite el último registro o null
     */
    fun getLastConnection(): Flow<ConnectionHistory?> = dao.getLastConnection()

    /**
     * Elimina un registro específico.
     *
     * @param history Registro a eliminar
     */
    suspend fun delete(history: ConnectionHistory) = dao.delete(history)

    /**
     * Elimina todo el historial.
     */
    suspend fun deleteAll() = dao.deleteAll()

    /**
     * Elimina registros más antiguos que una fecha.
     *
     * @param timestamp Timestamp límite
     */
    suspend fun deleteOlderThan(timestamp: Long) = dao.deleteOlderThan(timestamp)
}
