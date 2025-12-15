package com.parkingate.controller.util

import android.content.Context
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import androidx.compose.ui.hapticfeedback.HapticFeedbackType

/**
 * Gestor de feedback háptico para la aplicación.
 *
 * Proporciona diferentes tipos de vibraciones para mejorar la experiencia
 * del usuario y comunicar estados de forma táctil.
 *
 * ## Tipos de feedback:
 * - **Click**: Vibración corta para toques de botones
 * - **Success**: Patrón de vibración para operación exitosa
 * - **Error**: Patrón de vibración para errores
 * - **Warning**: Patrón de vibración para advertencias
 * - **LongPress**: Vibración para pulsaciones largas
 *
 * ## Compatibilidad:
 * - Android 12+ (API 31): Efectos de vibración predefinidos
 * - Android 8+ (API 26): VibrationEffect con amplitud
 * - Android < 8: Vibración simple (deprecated pero funcional)
 */
class HapticFeedbackManager(context: Context) {

    private val vibrator: Vibrator = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        val vibratorManager = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as VibratorManager
        vibratorManager.defaultVibrator
    } else {
        @Suppress("DEPRECATION")
        context.getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
    }

    /**
     * Vibración para click de botón.
     *
     * Duración: 50ms
     * Intensidad: Media
     */
    fun performClick() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val effect = VibrationEffect.createOneShot(50, VibrationEffect.DEFAULT_AMPLITUDE)
            vibrator.vibrate(effect)
        } else {
            @Suppress("DEPRECATION")
            vibrator.vibrate(50)
        }
    }

    /**
     * Vibración para operación exitosa.
     *
     * Patrón: Dos vibraciones cortas
     * Duración total: ~300ms
     */
    fun performSuccess() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            // Patrón: pausa 0ms, vibrar 100ms, pausa 50ms, vibrar 100ms
            val timings = longArrayOf(0, 100, 50, 100)
            val amplitudes = intArrayOf(0, 200, 0, 255)
            val effect = VibrationEffect.createWaveform(timings, amplitudes, -1)
            vibrator.vibrate(effect)
        } else {
            @Suppress("DEPRECATION")
            val pattern = longArrayOf(0, 100, 50, 100)
            vibrator.vibrate(pattern, -1)
        }
    }

    /**
     * Vibración para error.
     *
     * Patrón: Tres vibraciones cortas rápidas
     * Duración total: ~400ms
     */
    fun performError() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            // Patrón: pausa, vibrar, pausa, vibrar, pausa, vibrar
            val timings = longArrayOf(0, 80, 50, 80, 50, 80)
            val amplitudes = intArrayOf(0, 255, 0, 255, 0, 255)
            val effect = VibrationEffect.createWaveform(timings, amplitudes, -1)
            vibrator.vibrate(effect)
        } else {
            @Suppress("DEPRECATION")
            val pattern = longArrayOf(0, 80, 50, 80, 50, 80)
            vibrator.vibrate(pattern, -1)
        }
    }

    /**
     * Vibración para advertencia.
     *
     * Patrón: Una vibración larga
     * Duración: 200ms
     */
    fun performWarning() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val effect = VibrationEffect.createOneShot(200, 180)
            vibrator.vibrate(effect)
        } else {
            @Suppress("DEPRECATION")
            vibrator.vibrate(200)
        }
    }

    /**
     * Vibración para pulsación larga.
     *
     * Duración: 100ms
     * Intensidad: Alta
     */
    fun performLongPress() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val effect = VibrationEffect.createOneShot(100, 255)
            vibrator.vibrate(effect)
        } else {
            @Suppress("DEPRECATION")
            vibrator.vibrate(100)
        }
    }

    /**
     * Vibración para proceso en curso.
     *
     * Patrón: Pulsaciones suaves continuas
     * Útil para indicar que algo está pasando
     */
    fun performProcessing() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val timings = longArrayOf(0, 50, 100, 50)
            val amplitudes = intArrayOf(0, 100, 0, 100)
            val effect = VibrationEffect.createWaveform(timings, amplitudes, -1)
            vibrator.vibrate(effect)
        } else {
            @Suppress("DEPRECATION")
            val pattern = longArrayOf(0, 50, 100, 50)
            vibrator.vibrate(pattern, -1)
        }
    }

    /**
     * Cancela cualquier vibración en curso.
     */
    fun cancel() {
        vibrator.cancel()
    }
}
