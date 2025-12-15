package com.parkingate.controller.data

import android.content.Context
import kotlinx.coroutines.flow.Flow

/**
 * Repositorio para gestionar dispositivos BLE guardados.
 *
 * Proporciona una capa de abstracción entre el ViewModel y la base de datos.
 *
 * @property context Contexto de la aplicación
 */
class SavedDeviceRepository(context: Context) {

    private val dao: SavedDeviceDao =
        AppDatabase.getDatabase(context).savedDeviceDao()

    /**
     * Agrega un nuevo dispositivo.
     *
     * @param name Nombre del dispositivo
     * @param address Dirección MAC
     * @param serviceUuid UUID del servicio (opcional)
     * @param characteristicUuid UUID de la característica (opcional)
     * @param notes Notas adicionales (opcional)
     * @param colorHex Color en formato hexadecimal (opcional)
     * @param setAsActive Si true, establece este dispositivo como activo
     * @return ID del dispositivo creado
     */
    suspend fun addDevice(
        name: String,
        address: String,
        serviceUuid: String? = null,
        characteristicUuid: String? = null,
        notes: String? = null,
        colorHex: String? = null,
        setAsActive: Boolean = false
    ): Long {
        val device = SavedDevice(
            name = name,
            address = address,
            serviceUuid = serviceUuid,
            characteristicUuid = characteristicUuid,
            notes = notes,
            colorHex = colorHex,
            isActive = setAsActive
        )

        val id = dao.insert(device)

        // Si se debe establecer como activo, actualizar
        if (setAsActive) {
            dao.setActiveDevice(id)
        }

        return id
    }

    /**
     * Actualiza un dispositivo existente.
     *
     * @param device Dispositivo con los nuevos valores
     */
    suspend fun updateDevice(device: SavedDevice) = dao.update(device)

    /**
     * Elimina un dispositivo.
     *
     * @param device Dispositivo a eliminar
     */
    suspend fun deleteDevice(device: SavedDevice) = dao.delete(device)

    /**
     * Obtiene todos los dispositivos guardados.
     *
     * @return Flow que emite la lista de dispositivos
     */
    fun getAllDevices(): Flow<List<SavedDevice>> = dao.getAllDevices()

    /**
     * Obtiene el dispositivo activo actual.
     *
     * @return Flow que emite el dispositivo activo o null
     */
    fun getActiveDevice(): Flow<SavedDevice?> = dao.getActiveDevice()

    /**
     * Obtiene un dispositivo por ID.
     *
     * @param id ID del dispositivo
     * @return Flow que emite el dispositivo o null
     */
    fun getDeviceById(id: Long): Flow<SavedDevice?> = dao.getDeviceById(id)

    /**
     * Obtiene un dispositivo por dirección MAC.
     *
     * @param address Dirección MAC
     * @return Flow que emite el dispositivo o null
     */
    fun getDeviceByAddress(address: String): Flow<SavedDevice?> =
        dao.getDeviceByAddress(address)

    /**
     * Establece un dispositivo como activo.
     *
     * Automáticamente desactiva los demás dispositivos.
     *
     * @param deviceId ID del dispositivo a activar
     */
    suspend fun setActiveDevice(deviceId: Long) {
        dao.setActiveDevice(deviceId)
        dao.updateLastUsed(deviceId)
    }

    /**
     * Actualiza el timestamp de último uso.
     *
     * @param deviceId ID del dispositivo
     */
    suspend fun updateLastUsed(deviceId: Long) =
        dao.updateLastUsed(deviceId)

    /**
     * Obtiene el número total de dispositivos guardados.
     *
     * @return Flow que emite el conteo
     */
    fun getDeviceCount(): Flow<Int> = dao.getDeviceCount()

    /**
     * Elimina todos los dispositivos.
     */
    suspend fun deleteAll() = dao.deleteAll()
}
