package com.parkingate.controller.data

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

/**
 * DAO para gestionar dispositivos BLE guardados.
 *
 * Proporciona operaciones CRUD completas para dispositivos guardados.
 */
@Dao
interface SavedDeviceDao {

    /**
     * Inserta un nuevo dispositivo.
     *
     * @param device Dispositivo a insertar
     * @return ID del dispositivo insertado
     */
    @Insert
    suspend fun insert(device: SavedDevice): Long

    /**
     * Actualiza un dispositivo existente.
     *
     * @param device Dispositivo a actualizar
     */
    @Update
    suspend fun update(device: SavedDevice)

    /**
     * Elimina un dispositivo.
     *
     * @param device Dispositivo a eliminar
     */
    @Delete
    suspend fun delete(device: SavedDevice)

    /**
     * Obtiene todos los dispositivos guardados.
     *
     * @return Flow que emite la lista de todos los dispositivos
     */
    @Query("SELECT * FROM saved_devices ORDER BY lastUsedTimestamp DESC")
    fun getAllDevices(): Flow<List<SavedDevice>>

    /**
     * Obtiene el dispositivo activo.
     *
     * @return Flow que emite el dispositivo activo o null
     */
    @Query("SELECT * FROM saved_devices WHERE isActive = 1 LIMIT 1")
    fun getActiveDevice(): Flow<SavedDevice?>

    /**
     * Obtiene un dispositivo por ID.
     *
     * @param id ID del dispositivo
     * @return Flow que emite el dispositivo o null
     */
    @Query("SELECT * FROM saved_devices WHERE id = :id")
    fun getDeviceById(id: Long): Flow<SavedDevice?>

    /**
     * Obtiene un dispositivo por dirección MAC.
     *
     * @param address Dirección MAC del dispositivo
     * @return Flow que emite el dispositivo o null
     */
    @Query("SELECT * FROM saved_devices WHERE address = :address LIMIT 1")
    fun getDeviceByAddress(address: String): Flow<SavedDevice?>

    /**
     * Establece un dispositivo como activo y desactiva los demás.
     *
     * Primero desactiva todos los dispositivos, luego activa el especificado.
     *
     * @param deviceId ID del dispositivo a activar
     */
    @Query("UPDATE saved_devices SET isActive = CASE WHEN id = :deviceId THEN 1 ELSE 0 END")
    suspend fun setActiveDevice(deviceId: Long)

    /**
     * Actualiza el timestamp de último uso de un dispositivo.
     *
     * @param deviceId ID del dispositivo
     * @param timestamp Nuevo timestamp
     */
    @Query("UPDATE saved_devices SET lastUsedTimestamp = :timestamp WHERE id = :deviceId")
    suspend fun updateLastUsed(deviceId: Long, timestamp: Long = System.currentTimeMillis())

    /**
     * Obtiene el conteo total de dispositivos guardados.
     *
     * @return Flow que emite el número de dispositivos
     */
    @Query("SELECT COUNT(*) FROM saved_devices")
    fun getDeviceCount(): Flow<Int>

    /**
     * Elimina todos los dispositivos.
     */
    @Query("DELETE FROM saved_devices")
    suspend fun deleteAll()
}
