package com.parkingate.controller.data

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Entidad que representa un dispositivo BLE guardado.
 *
 * Permite al usuario gestionar múltiples dispositivos ESP32
 * (por ejemplo, diferentes plumas de estacionamiento en casa, trabajo, etc.)
 *
 * ## Campos:
 * - **id**: ID autogenerado único
 * - **name**: Nombre personalizado del dispositivo (ej: "Pluma Casa")
 * - **address**: Dirección MAC del dispositivo BLE
 * - **serviceUuid**: UUID del servicio GATT (opcional)
 * - **characteristicUuid**: UUID de la característica GATT (opcional)
 * - **isActive**: Indica si es el dispositivo activo actualmente
 * - **addedTimestamp**: Cuándo se agregó el dispositivo
 * - **lastUsedTimestamp**: Última vez que se usó
 * - **notes**: Notas adicionales del usuario
 */
@Entity(tableName = "saved_devices")
data class SavedDevice(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,

    /**
     * Nombre personalizado del dispositivo
     */
    val name: String,

    /**
     * Dirección MAC del dispositivo BLE (ej: "AA:BB:CC:DD:EE:FF")
     */
    val address: String,

    /**
     * UUID del servicio GATT (opcional)
     */
    val serviceUuid: String? = null,

    /**
     * UUID de la característica GATT (opcional)
     */
    val characteristicUuid: String? = null,

    /**
     * Indica si este es el dispositivo activo
     * Solo un dispositivo puede estar activo a la vez
     */
    val isActive: Boolean = false,

    /**
     * Timestamp de cuándo se agregó
     */
    val addedTimestamp: Long = System.currentTimeMillis(),

    /**
     * Timestamp de la última vez que se usó
     */
    val lastUsedTimestamp: Long = System.currentTimeMillis(),

    /**
     * Notas adicionales del usuario
     */
    val notes: String? = null,

    /**
     * Icono o color personalizado (opcional)
     */
    val colorHex: String? = null
)
