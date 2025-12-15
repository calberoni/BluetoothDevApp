package com.parkingate.controller.security

import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Base64
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec

/**
 * Gestor de criptografía para encriptar/desencriptar datos sensibles.
 *
 * Utiliza:
 * - **AES-256-GCM**: Algoritmo de encriptación simétrica moderno y seguro
 * - **Android KeyStore**: Almacenamiento seguro de claves en hardware (si está disponible)
 * - **GCM Mode**: Garantiza autenticidad e integridad además de confidencialidad
 *
 * ## Seguridad:
 * - La clave nunca sale del KeyStore
 * - GCM proporciona autenticación de datos (detecta modificaciones)
 * - IV (Initialization Vector) único por cada encriptación
 *
 * ## Uso:
 * ```kotlin
 * val crypto = CryptoManager()
 * val encrypted = crypto.encrypt("datos sensibles")
 * val decrypted = crypto.decrypt(encrypted)
 * ```
 */
class CryptoManager {

    companion object {
        private const val KEYSTORE_PROVIDER = "AndroidKeyStore"
        private const val KEY_ALIAS = "bluetooth_dev_key"
        private const val TRANSFORMATION = "AES/GCM/NoPadding"
        private const val GCM_TAG_LENGTH = 128
        private const val IV_SEPARATOR = "]"
    }

    private val keyStore: KeyStore = KeyStore.getInstance(KEYSTORE_PROVIDER).apply {
        load(null)
    }

    init {
        // Crear la clave si no existe
        if (!keyStore.containsAlias(KEY_ALIAS)) {
            createKey()
        }
    }

    /**
     * Crea una nueva clave AES-256 en el KeyStore.
     *
     * La clave está protegida por hardware (si el dispositivo lo soporta)
     * y nunca puede ser extraída del KeyStore.
     */
    private fun createKey() {
        val keyGenerator = KeyGenerator.getInstance(
            KeyProperties.KEY_ALGORITHM_AES,
            KEYSTORE_PROVIDER
        )

        val spec = KeyGenParameterSpec.Builder(
            KEY_ALIAS,
            KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT
        )
            .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
            .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
            .setKeySize(256)
            .setRandomizedEncryptionRequired(true) // IV aleatorio por seguridad
            .build()

        keyGenerator.init(spec)
        keyGenerator.generateKey()
    }

    /**
     * Obtiene la clave secreta del KeyStore.
     *
     * @return Clave secreta AES-256
     */
    private fun getKey(): SecretKey {
        return keyStore.getKey(KEY_ALIAS, null) as SecretKey
    }

    /**
     * Encripta un texto plano usando AES-256-GCM.
     *
     * @param plainText Texto a encriptar
     * @return Texto encriptado en Base64, con el formato: [IV]CipherText
     *         El IV se incluye al inicio para poder desencriptar después
     */
    fun encrypt(plainText: String): String {
        val cipher = Cipher.getInstance(TRANSFORMATION)
        cipher.init(Cipher.ENCRYPT_MODE, getKey())

        val iv = cipher.iv
        val cipherText = cipher.doFinal(plainText.toByteArray(Charsets.UTF_8))

        // Formato: [IV_Base64]CipherText_Base64
        val ivBase64 = Base64.encodeToString(iv, Base64.NO_WRAP)
        val cipherTextBase64 = Base64.encodeToString(cipherText, Base64.NO_WRAP)

        return "$ivBase64$IV_SEPARATOR$cipherTextBase64"
    }

    /**
     * Desencripta un texto previamente encriptado.
     *
     * @param encryptedText Texto encriptado (debe estar en el formato [IV]CipherText)
     * @return Texto plano desencriptado
     * @throws Exception si el texto está corrupto o fue modificado
     */
    fun decrypt(encryptedText: String): String {
        // Separar IV y CipherText
        val parts = encryptedText.split(IV_SEPARATOR)
        if (parts.size != 2) {
            throw IllegalArgumentException("Formato de texto encriptado inválido")
        }

        val iv = Base64.decode(parts[0], Base64.NO_WRAP)
        val cipherText = Base64.decode(parts[1], Base64.NO_WRAP)

        val cipher = Cipher.getInstance(TRANSFORMATION)
        val spec = GCMParameterSpec(GCM_TAG_LENGTH, iv)
        cipher.init(Cipher.DECRYPT_MODE, getKey(), spec)

        val plainText = cipher.doFinal(cipherText)
        return String(plainText, Charsets.UTF_8)
    }

    /**
     * Verifica si un texto está en formato encriptado válido.
     *
     * @param text Texto a verificar
     * @return true si parece estar encriptado, false en caso contrario
     */
    fun isEncrypted(text: String): Boolean {
        return text.contains(IV_SEPARATOR) && text.split(IV_SEPARATOR).size == 2
    }

    /**
     * Elimina la clave del KeyStore.
     *
     * ADVERTENCIA: Esto hará que todos los datos encriptados con esta clave
     * sean irrecuperables. Usar solo para reset completo.
     */
    fun deleteKey() {
        if (keyStore.containsAlias(KEY_ALIAS)) {
            keyStore.deleteEntry(KEY_ALIAS)
        }
    }
}
