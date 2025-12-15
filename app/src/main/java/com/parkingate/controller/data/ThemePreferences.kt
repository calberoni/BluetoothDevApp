package com.parkingate.controller.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.themeDataStore: DataStore<Preferences> by preferencesDataStore(
    name = "theme_preferences"
)

/**
 * Gestor de preferencias de tema.
 *
 * Almacena la preferencia del usuario sobre el tema oscuro/claro.
 */
class ThemePreferences(private val context: Context) {

    companion object {
        private val DARK_THEME_KEY = booleanPreferencesKey("dark_theme")
    }

    /**
     * Flow que emite true si el tema oscuro está activo.
     */
    val isDarkTheme: Flow<Boolean> = context.themeDataStore.data.map { preferences ->
        preferences[DARK_THEME_KEY] ?: false  // Por defecto: tema claro
    }

    /**
     * Guarda la preferencia de tema oscuro.
     *
     * @param isDark true para tema oscuro, false para tema claro
     */
    suspend fun setDarkTheme(isDark: Boolean) {
        context.themeDataStore.edit { preferences ->
            preferences[DARK_THEME_KEY] = isDark
        }
    }

    /**
     * Alterna entre tema oscuro y claro.
     */
    suspend fun toggleTheme() {
        context.themeDataStore.edit { preferences ->
            val currentValue = preferences[DARK_THEME_KEY] ?: false
            preferences[DARK_THEME_KEY] = !currentValue
        }
    }
}
