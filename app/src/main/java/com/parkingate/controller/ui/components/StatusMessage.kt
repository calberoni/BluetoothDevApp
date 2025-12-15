package com.parkingate.controller.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.parkingate.controller.ble.BleConnectionState
import com.parkingate.controller.ui.theme.Error
import com.parkingate.controller.ui.theme.Success

/**
 * Componente que muestra el mensaje de estado actual.
 *
 * Muestra diferentes textos y colores según el estado de la conexión BLE:
 * - Idle: Texto normal
 * - Processing: Texto normal
 * - Success: Texto verde
 * - Error: Texto rojo con opción de reintentar
 *
 * @param state Estado actual de la conexión BLE
 * @param message Mensaje a mostrar
 * @param onRetry Callback para reintentar (solo visible en estado Error)
 * @param modifier Modificador de composición
 */
@Composable
fun StatusMessage(
    state: BleConnectionState,
    message: String,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = message,
            fontSize = 18.sp,
            fontWeight = FontWeight.Medium,
            textAlign = TextAlign.Center,
            color = when (state) {
                is BleConnectionState.Success -> Success
                is BleConnectionState.Error -> Error
                else -> MaterialTheme.colorScheme.onBackground
            }
        )

        // Mostrar botón de reintentar solo en estado de error
        AnimatedVisibility(
            visible = state is BleConnectionState.Error && (state as? BleConnectionState.Error)?.canRetry == true,
            enter = fadeIn(),
            exit = fadeOut()
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Spacer(modifier = Modifier.height(8.dp))
                TextButton(onClick = onRetry) {
                    Text(
                        text = "Reintentar",
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}
