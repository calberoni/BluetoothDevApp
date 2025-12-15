package com.parkingate.controller.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Delete
import kotlinx.coroutines.flow.Flow

/**
 * DAO (Data Access Object) para acceder al historial de conexiones.
 *
 * Proporciona métodos para:
 * - Insertar nuevos registros
 * - Consultar el historial completo
 * - Obtener estadísticas
 * - Eliminar registros
 */
@Dao
interface ConnectionHistoryDao {

    /**
     * Inserta un nuevo registro en el historial.
     *
     * @param history Registro de conexión a insertar
     * @return ID del registro insertado
     */
    @Insert
    suspend fun insert(history: ConnectionHistory): Long

    /**
     * Obtiene todo el historial ordenado por timestamp descendente (más reciente primero).
     *
     * @return Flow que emite la lista completa del historial
     */
    @Query("SELECT * FROM connection_history ORDER BY timestamp DESC")
    fun getAllHistory(): Flow<List<ConnectionHistory>>

    /**
     * Obtiene los N registros más recientes.
     *
     * @param limit Número máximo de registros a obtener
     * @return Flow que emite la lista de registros recientes
     */
    @Query("SELECT * FROM connection_history ORDER BY timestamp DESC LIMIT :limit")
    fun getRecentHistory(limit: Int = 20): Flow<List<ConnectionHistory>>

    /**
     * Obtiene registros dentro de un rango de fechas.
     *
     * @param startTime Timestamp inicial
     * @param endTime Timestamp final
     * @return Flow que emite la lista de registros en el rango
     */
    @Query("SELECT * FROM connection_history WHERE timestamp BETWEEN :startTime AND :endTime ORDER BY timestamp DESC")
    fun getHistoryInRange(startTime: Long, endTime: Long): Flow<List<ConnectionHistory>>

    /**
     * Obtiene el conteo total de conexiones.
     *
     * @return Flow que emite el número total de registros
     */
    @Query("SELECT COUNT(*) FROM connection_history")
    fun getTotalConnectionCount(): Flow<Int>

    /**
     * Obtiene el conteo de conexiones exitosas.
     *
     * @return Flow que emite el número de conexiones exitosas
     */
    @Query("SELECT COUNT(*) FROM connection_history WHERE result = 'SUCCESS'")
    fun getSuccessfulConnectionCount(): Flow<Int>

    /**
     * Obtiene el RSSI promedio de todas las conexiones.
     *
     * @return Flow que emite el RSSI promedio
     */
    @Query("SELECT AVG(rssi) FROM connection_history")
    fun getAverageRssi(): Flow<Double?>

    /**
     * Obtiene el último registro.
     *
     * @return Flow que emite el último registro o null si no hay registros
     */
    @Query("SELECT * FROM connection_history ORDER BY timestamp DESC LIMIT 1")
    fun getLastConnection(): Flow<ConnectionHistory?>

    /**
     * Elimina un registro específico.
     *
     * @param history Registro a eliminar
     */
    @Delete
    suspend fun delete(history: ConnectionHistory)

    /**
     * Elimina todos los registros.
     */
    @Query("DELETE FROM connection_history")
    suspend fun deleteAll()

    /**
     * Elimina registros más antiguos que una fecha específica.
     *
     * @param timestamp Timestamp límite (se eliminan registros anteriores)
     */
    @Query("DELETE FROM connection_history WHERE timestamp < :timestamp")
    suspend fun deleteOlderThan(timestamp: Long)
}
