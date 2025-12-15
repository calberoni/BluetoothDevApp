package com.parkingate.controller.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters

/**
 * Base de datos Room de la aplicación.
 *
 * Contiene todas las entidades y DAOs necesarios para el almacenamiento local.
 *
 * ## Entidades:
 * - ConnectionHistory: Historial de conexiones BLE
 * - SavedDevice: Dispositivos BLE guardados
 *
 * ## DAOs:
 * - ConnectionHistoryDao: Acceso al historial de conexiones
 * - SavedDeviceDao: Acceso a dispositivos guardados
 */
@Database(
    entities = [ConnectionHistory::class, SavedDevice::class],
    version = 2,
    exportSchema = false
)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {

    /**
     * Proporciona acceso al DAO de historial de conexiones.
     */
    abstract fun connectionHistoryDao(): ConnectionHistoryDao

    /**
     * Proporciona acceso al DAO de dispositivos guardados.
     */
    abstract fun savedDeviceDao(): SavedDeviceDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        /**
         * Obtiene la instancia singleton de la base de datos.
         *
         * Usa el patrón singleton con doble verificación para garantizar
         * que solo exista una instancia de la base de datos en la app.
         *
         * @param context Contexto de la aplicación
         * @return Instancia de la base de datos
         */
        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "bluetooth_dev_database"
                )
                    .fallbackToDestructiveMigration() // Solo para desarrollo
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}

/**
 * Conversores de tipos para Room.
 *
 * Room solo soporta tipos primitivos por defecto, por lo que necesitamos
 * conversores para tipos personalizados como enums.
 */
class Converters {
    @androidx.room.TypeConverter
    fun fromConnectionResult(value: ConnectionResult): String {
        return value.name
    }

    @androidx.room.TypeConverter
    fun toConnectionResult(value: String): ConnectionResult {
        return ConnectionResult.valueOf(value)
    }
}
