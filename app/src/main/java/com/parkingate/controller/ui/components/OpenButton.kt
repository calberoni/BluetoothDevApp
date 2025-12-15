package com.parkingate.controller.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * Botón principal "ABRIR" con animaciones.
 *
 * Este componente muestra:
 * - Botón circular grande con texto "ABRIR"
 * - Animación de pulso cuando está procesando
 * - Indicador de progreso circular durante operación
 * - Cambio de color según el estado
 *
 * @param onClick Callback cuando se presiona el botón
 * @param isProcessing Indica si hay una operación en curso
 * @param enabled Indica si el botón está habilitado
 * @param modifier Modificador de composición
 */
@Composable
fun OpenButton(
    onClick: () -> Unit,
    isProcessing: Boolean,
    enabled: Boolean,
    modifier: Modifier = Modifier
) {
    // Animación de pulso cuando está procesando (mejorada con spring)
    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 1.08f,
        animationSpec = infiniteRepeatable(
            animation = tween(800, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "scale"
    )

    // Animación suave de escala al presionar (spring animation)
    val targetScale = if (isProcessing) pulseScale else 1f
    val animatedScale by animateFloatAsState(
        targetValue = targetScale,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessLow
        ),
        label = "button_scale"
    )

    // Animación de color del botón
    val buttonColor by animateColorAsState(
        targetValue = if (enabled && !isProcessing) {
            MaterialTheme.colorScheme.primary
        } else {
            MaterialTheme.colorScheme.primary.copy(alpha = 0.6f)
        },
        animationSpec = tween(300),
        label = "button_color"
    )

    Box(
        modifier = modifier,
        contentAlignment = Alignment.Center
    ) {
        Button(
            onClick = onClick,
            enabled = enabled && !isProcessing,
            shape = CircleShape,
            colors = ButtonDefaults.buttonColors(
                containerColor = buttonColor,
                contentColor = MaterialTheme.colorScheme.onPrimary,
                disabledContainerColor = buttonColor,
                disabledContentColor = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.7f)
            ),
            elevation = ButtonDefaults.buttonElevation(
                defaultElevation = 8.dp,
                pressedElevation = 12.dp,
                disabledElevation = 4.dp
            ),
            modifier = Modifier
                .size(200.dp)
                .scale(animatedScale)
        ) {
            if (isProcessing) {
                CircularProgressIndicator(
                    color = MaterialTheme.colorScheme.onPrimary,
                    strokeWidth = 4.dp,
                    modifier = Modifier.size(48.dp)
                )
            } else {
                Text(
                    text = "ABRIR",
                    fontSize = 32.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 2.sp
                )
            }
        }
    }
}
