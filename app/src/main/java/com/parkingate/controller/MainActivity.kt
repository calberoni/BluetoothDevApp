package com.parkingate.controller

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.lifecycle.viewmodel.compose.viewModel
import com.parkingate.controller.ui.MainScreen
import com.parkingate.controller.ui.theme.BluetoothDevAppTheme
import com.parkingate.controller.viewmodel.MainViewModel

/**
 * Activity principal de la aplicación.
 *
 * Esta Activity es el punto de entrada de la aplicación y configura
 * Jetpack Compose como sistema de UI.
 *
 * ## Responsabilidades:
 * - Configurar el tema de la aplicación
 * - Inicializar la pantalla principal con Compose
 * - Manejar el ciclo de vida de la Activity
 *
 * La aplicación usa:
 * - Jetpack Compose para UI declarativa
 * - MVVM para arquitectura
 * - StateFlow para gestión de estado reactivo
 * - Coroutines para operaciones asíncronas
 */
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            val viewModel: MainViewModel = viewModel()
            val isDarkTheme by viewModel.isDarkTheme.collectAsState(initial = false)

            BluetoothDevAppTheme(darkTheme = isDarkTheme) {
                MainScreen()
            }
        }
    }
}
