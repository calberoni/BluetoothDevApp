package com.parkingate.controller.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.parkingate.controller.security.CryptoManager
import com.parkingate.controller.util.Constants
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import java.util.UUID

/**
 * Extensión para crear una instancia de DataStore a nivel de Context.
 *
 * DataStore es una solución de almacenamiento de datos moderna que reemplaza
 * SharedPreferences con una API basada en Kotlin Coroutines y Flow.
 */
private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(
    name = Constants.DATASTORE_NAME
)

/**
 * Repositorio para gestionar el UUID único del usuario.
 *
 * Este repositorio es responsable de:
 * - Generar un UUID único la primera vez que se usa la app
 * - Almacenar el UUID de forma persistente usando DataStore
 * - Proporcionar acceso al UUID a través de Flow (reactivo)
 *
 * El UUID generado tiene el formato estándar:
 * "550e8400-e29b-41d4-a716-446655440000"
 *
 * Este UUID se envía al ESP32 cada vez que el usuario presiona el botón "ABRIR"
 * para identificar al usuario y autorizar la apertura de la pluma.
 *
 * ## ¿Por qué usar UUID?
 *
 * - **Único**: Prácticamente imposible que dos dispositivos generen el mismo UUID
 * - **Sin servidor**: No requiere backend para generar IDs
 * - **Privacidad**: No contiene información personal del usuario
 * - **Estándar**: Formato ampliamente reconocido y soportado
 *
 * ## ¿Qué es DataStore?
 *
 * DataStore es la solución moderna de Android para almacenamiento persistente:
 * - Basado en Kotlin Coroutines y Flow
 * - Seguro para operaciones asíncronas
 * - Maneja errores de forma consistente
 * - Tipo seguro con Preferences DataStore
 *
 * @property context Contexto de la aplicación para acceder a DataStore
 */
class UserRepository(private val context: Context) {

    companion object {
        private val USER_UUID_KEY = stringPreferencesKey(Constants.USER_UUID_KEY)
    }

    /**
     * Gestor de criptografía para encriptar/desencriptar el UUID.
     *
     * El UUID se almacena encriptado en DataStore para mayor seguridad.
     */
    private val cryptoManager = CryptoManager()

    /**
     * Flow que emite el UUID del usuario (desencriptado).
     *
     * Si no existe un UUID guardado, genera uno nuevo automáticamente
     * y lo almacena para uso futuro (encriptado).
     *
     * Flow es reactivo: cualquier cambio en el valor se emitirá
     * automáticamente a todos los suscriptores.
     *
     * El UUID se almacena encriptado con AES-256-GCM en DataStore
     * y se desencripta automáticamente al leerlo.
     */
    val userUuid: Flow<String> = context.dataStore.data.map { preferences ->
        val encryptedUuid = preferences[USER_UUID_KEY]
        if (encryptedUuid != null) {
            try {
                // Desencriptar UUID almacenado
                cryptoManager.decrypt(encryptedUuid)
            } catch (e: Exception) {
                // Si falla la desencriptación (clave corrupta, etc.),
                // generar un nuevo UUID
                generateAndSaveUuid()
            }
        } else {
            // No hay UUID guardado, generar uno nuevo
            generateAndSaveUuid()
        }
    }

    /**
     * Obtiene el UUID del usuario de forma suspendida.
     *
     * Esta función es útil cuando necesitas el UUID en una coroutine
     * y no quieres colectar el Flow.
     *
     * @return UUID del usuario como String
     */
    suspend fun getUserUuid(): String {
        return userUuid.first()
    }

    /**
     * Genera un nuevo UUID y lo guarda en DataStore (encriptado).
     *
     * El UUID se genera usando java.util.UUID.randomUUID() que
     * utiliza un generador de números aleatorios criptográficamente seguro.
     *
     * Formato del UUID:
     * - 8 caracteres hexadecimales
     * - guión
     * - 4 caracteres hexadecimales
     * - guión
     * - 4 caracteres hexadecimales
     * - guión
     * - 4 caracteres hexadecimales
     * - guión
     * - 12 caracteres hexadecimales
     *
     * Ejemplo: "550e8400-e29b-41d4-a716-446655440000"
     *
     * El UUID se encripta con AES-256-GCM antes de almacenarlo en DataStore.
     *
     * @return El UUID generado (sin encriptar) como String
     */
    private suspend fun generateAndSaveUuid(): String {
        val newUuid = UUID.randomUUID().toString()

        // Encriptar el UUID antes de guardarlo
        val encryptedUuid = cryptoManager.encrypt(newUuid)

        context.dataStore.edit { preferences ->
            preferences[USER_UUID_KEY] = encryptedUuid
        }

        return newUuid  // Retornar el UUID sin encriptar
    }

    /**
     * Establece un UUID personalizado.
     *
     * Permite al usuario configurar manualmente el UUID a enviar.
     *
     * @param uuid UUID en formato string (será encriptado antes de guardarse)
     */
    suspend fun setCustomUuid(uuid: String) {
        val encryptedUuid = cryptoManager.encrypt(uuid)
        context.dataStore.edit { preferences ->
            preferences[USER_UUID_KEY] = encryptedUuid
        }
    }

    /**
     * Elimina el UUID almacenado (útil para testing o reset).
     *
     * Después de llamar a esta función, la próxima vez que se acceda
     * a userUuid se generará un nuevo UUID.
     */
    suspend fun clearUuid() {
        context.dataStore.edit { preferences ->
            preferences.remove(USER_UUID_KEY)
        }
    }

    /**
     * Verifica si existe un UUID almacenado.
     *
     * @return true si existe un UUID, false en caso contrario
     */
    suspend fun hasUuid(): Boolean {
        val preferences = context.dataStore.data.first()
        return preferences.contains(USER_UUID_KEY)
    }
}
