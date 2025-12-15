package com.parkingate.controller.ui

import android.Manifest
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.background
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import kotlinx.coroutines.launch
import androidx.compose.foundation.layout.size
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.MultiplePermissionsState
import com.google.accompanist.permissions.isGranted
import com.google.accompanist.permissions.rememberMultiplePermissionsState
import com.parkingate.controller.ble.BleConnectionState
import com.parkingate.controller.ui.components.OpenButton
import com.parkingate.controller.ui.components.StatusMessage
import com.parkingate.controller.viewmodel.MainViewModel

/**
 * Pantalla principal de la aplicación.
 *
 * Esta pantalla muestra:
 * - Título de la app en la parte superior
 * - Botón central grande "ABRIR"
 * - Mensaje de estado debajo del botón
 * - Manejo de permisos BLE
 *
 * ## Diseño:
 * - Fondo blanco limpio
 * - Elementos en color naranja (#FF6F00)
 * - Diseño minimalista y centrado
 * - Animaciones suaves entre estados
 *
 * ## Flujo de usuario:
 * 1. Usuario abre la app
 * 2. Se solicitan permisos BLE si no están concedidos
 * 3. Usuario presiona botón "ABRIR"
 * 4. App escanea, conecta y envía comando
 * 5. Se muestra feedback visual en cada paso
 * 6. Al completar, vuelve a estado inicial
 *
 * @param viewModel ViewModel que gestiona la lógica de negocio
 */
@OptIn(ExperimentalPermissionsApi::class)
@Composable
fun MainScreen(
    viewModel: MainViewModel = viewModel()
) {
    val context = LocalContext.current
    val connectionState by viewModel.connectionState.collectAsState()
    val isProcessing by viewModel.isProcessing.collectAsState()
    val bleLog by viewModel.bleLog.collectAsState()
    val rssi by viewModel.rssi.collectAsState()
    val isDarkTheme by viewModel.isDarkTheme.collectAsState(initial = false)
    val currentUuid by viewModel.userUuid.collectAsState()

    var showConfigDialog by remember { mutableStateOf(false) }

    // Determinar permisos necesarios según la versión de Android
    val permissions = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        listOf(
            Manifest.permission.BLUETOOTH_SCAN,
            Manifest.permission.BLUETOOTH_CONNECT
        )
    } else {
        listOf(
            Manifest.permission.BLUETOOTH,
            Manifest.permission.BLUETOOTH_ADMIN,
            Manifest.permission.ACCESS_FINE_LOCATION
        )
    }

    val permissionsState = rememberMultiplePermissionsState(permissions)

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            // Título superior con botón de tema
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 32.dp)
            ) {
                // Fila con título y botones
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.Top
                ) {
                    // Botón de configuración (izquierda)
                    IconButton(
                        onClick = { showConfigDialog = true },
                        modifier = Modifier.padding(start = 8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Settings,
                            contentDescription = "Configuración",
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }

                    // Título centrado
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.weight(1f)
                    ) {
                        Text(
                            text = "KIGO Bluetooth Dev App",
                            fontSize = 36.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Text(
                            text = "BLE Development Tool",
                            fontSize = 16.sp,
                            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f)
                        )
                    }

                    // Botón de toggle de tema (derecha)
                    IconButton(
                        onClick = { viewModel.toggleTheme() },
                        modifier = Modifier.padding(end = 8.dp)
                    ) {
                        Text(
                            text = if (isDarkTheme) "☀️" else "🌙",
                            fontSize = 24.sp
                        )
                    }
                }

                // Indicador de señal RSSI
                if (rssi > -100) {
                    Spacer(modifier = Modifier.height(16.dp))
                    SignalStrengthIndicator(rssi = rssi)
                }
            }

            // Botón central y estado
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                OpenButton(
                    onClick = {
                        if (permissionsState.allPermissionsGranted) {
                            viewModel.onOpenButtonClick()
                        } else {
                            permissionsState.launchMultiplePermissionRequest()
                        }
                    },
                    isProcessing = isProcessing,
                    enabled = permissionsState.allPermissionsGranted
                )

                Spacer(modifier = Modifier.height(32.dp))

                StatusMessage(
                    state = connectionState,
                    message = if (permissionsState.allPermissionsGranted) {
                        viewModel.getStateMessage(connectionState)
                    } else {
                        "Permisos de Bluetooth necesarios"
                    },
                    onRetry = {
                        viewModel.resetState()
                        viewModel.onOpenButtonClick()
                    }
                )

                Spacer(modifier = Modifier.height(24.dp))

                // Área de logs BLE
                BleLogViewer(logs = bleLog)
            }

            // Espacio inferior
            Spacer(modifier = Modifier.height(16.dp))
        }

        // Diálogo de permisos
        PermissionDialog(permissionsState = permissionsState)

        // Diálogo de configuración
        if (showConfigDialog) {
            val scope = rememberCoroutineScope()
            ConfigDialog(
                currentUuid = currentUuid ?: "",
                onDismiss = { showConfigDialog = false },
                onSave = { newUuid ->
                    scope.launch {
                        if (viewModel.setCustomUuid(newUuid)) {
                            showConfigDialog = false
                        }
                    }
                },
                onReset = {
                    scope.launch {
                        viewModel.resetUuid()
                        showConfigDialog = false
                    }
                }
            )
        }
    }
}

/**
 * Diálogo que explica por qué se necesitan los permisos.
 *
 * Se muestra cuando:
 * - El usuario deniega los permisos
 * - Los permisos son necesarios para el funcionamiento
 *
 * @param permissionsState Estado de los permisos múltiples
 */
@OptIn(ExperimentalPermissionsApi::class)
@Composable
fun PermissionDialog(permissionsState: MultiplePermissionsState) {
    val context = LocalContext.current
    var showRationaleDialog by remember { mutableStateOf(false) }

    // Mostrar diálogo si se denegaron permisos
    if (!permissionsState.allPermissionsGranted && permissionsState.shouldShowRationale) {
        showRationaleDialog = true
    }

    if (showRationaleDialog) {
        AlertDialog(
            onDismissRequest = { showRationaleDialog = false },
            title = {
                Text(
                    text = "Permisos necesarios",
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Text(
                    text = "Esta aplicación necesita permisos de Bluetooth para conectarse " +
                            "a la pluma del estacionamiento. Sin estos permisos, no podrá " +
                            "abrir la pluma.\n\nPor favor, conceda los permisos para continuar.",
                    textAlign = TextAlign.Start
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        showRationaleDialog = false
                        permissionsState.launchMultiplePermissionRequest()
                    }
                ) {
                    Text("Conceder permisos")
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { showRationaleDialog = false }
                ) {
                    Text("Cancelar")
                }
            }
        )
    }

    // Mostrar diálogo para ir a configuración si los permisos se denegaron permanentemente
    if (!permissionsState.allPermissionsGranted && !permissionsState.shouldShowRationale) {
        var showSettingsDialog by remember { mutableStateOf(false) }

        if (permissionsState.permissions.any { !it.status.isGranted }) {
            showSettingsDialog = true
        }

        if (showSettingsDialog) {
            AlertDialog(
                onDismissRequest = { showSettingsDialog = false },
                title = {
                    Text(
                        text = "Permisos requeridos",
                        fontWeight = FontWeight.Bold
                    )
                },
                text = {
                    Text(
                        text = "Los permisos de Bluetooth son necesarios para el funcionamiento " +
                                "de la aplicación. Por favor, habilítelos en la configuración " +
                                "de la aplicación.",
                        textAlign = TextAlign.Start
                    )
                },
                confirmButton = {
                    TextButton(
                        onClick = {
                            showSettingsDialog = false
                            val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                                data = Uri.fromParts("package", context.packageName, null)
                            }
                            context.startActivity(intent)
                        }
                    ) {
                        Text("Ir a configuración")
                    }
                },
                dismissButton = {
                    TextButton(
                        onClick = { showSettingsDialog = false }
                    ) {
                        Text("Cancelar")
                    }
                }
            )
        }
    }
}

/**
 * Componente que muestra los logs BLE en tiempo real.
 *
 * @param logs Lista de mensajes de log para mostrar
 */
@Composable
fun BleLogViewer(logs: List<String>) {
    val listState = rememberLazyListState()

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
    ) {
        Text(
            text = "BLE Transaction Log",
            fontSize = 14.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.8f),
            modifier = Modifier.padding(bottom = 8.dp)
        )

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(200.dp)
                .background(
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
                    shape = RoundedCornerShape(8.dp)
                )
                .padding(12.dp)
        ) {
            LazyColumn(
                state = listState,
                modifier = Modifier.fillMaxSize(),
                reverseLayout = false
            ) {
                items(logs) { logMessage ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 2.dp)
                    ) {
                        Text(
                            text = "• ",
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = logMessage,
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.9f),
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }
        }
    }

    // Auto-scroll al último log
    androidx.compose.runtime.LaunchedEffect(logs.size) {
        if (logs.isNotEmpty()) {
            listState.animateScrollToItem(logs.size - 1)
        }
    }
}

/**
 * Componente que muestra la intensidad de señal BLE (RSSI).
 *
 * @param rssi Valor RSSI en dBm (-100 a 0)
 */
@Composable
fun SignalStrengthIndicator(rssi: Int) {
    // Calcular nivel de señal (0-4 barras)
    val signalLevel = when {
        rssi >= -50 -> 4  // Excelente
        rssi >= -60 -> 3  // Buena
        rssi >= -70 -> 2  // Regular
        rssi >= -80 -> 1  // Débil
        else -> 0         // Muy débil
    }

    val signalColor = when (signalLevel) {
        4 -> androidx.compose.ui.graphics.Color(0xFF4CAF50)  // Verde
        3 -> androidx.compose.ui.graphics.Color(0xFF8BC34A)  // Verde claro
        2 -> androidx.compose.ui.graphics.Color(0xFFFFC107)  // Amarillo
        1 -> androidx.compose.ui.graphics.Color(0xFFFF9800)  // Naranja
        else -> androidx.compose.ui.graphics.Color(0xFFF44336)  // Rojo
    }

    val signalText = when (signalLevel) {
        4 -> "Excelente"
        3 -> "Buena"
        2 -> "Regular"
        1 -> "Débil"
        else -> "Muy débil"
    }

    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        // Barras de señal
        repeat(4) { index ->
            Box(
                modifier = Modifier
                    .width(6.dp)
                    .height((8 + index * 4).dp)
                    .background(
                        color = if (index < signalLevel) signalColor
                        else MaterialTheme.colorScheme.onBackground.copy(alpha = 0.2f),
                        shape = RoundedCornerShape(2.dp)
                    )
            )
        }

        Spacer(modifier = Modifier.width(8.dp))

        // Texto descriptivo
        Column {
            Text(
                text = signalText,
                fontSize = 12.sp,
                fontWeight = FontWeight.Medium,
                color = signalColor
            )
            Text(
                text = "$rssi dBm",
                fontSize = 10.sp,
                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f)
            )
        }
    }
}

/**
 * Diálogo de configuración para editar UUID y otros ajustes.
 *
 * @param currentUuid UUID actual
 * @param onDismiss Callback cuando se cierra el diálogo
 * @param onSave Callback cuando se guarda el UUID
 * @param onReset Callback cuando se resetea el UUID
 */
@Composable
fun ConfigDialog(
    currentUuid: String,
    onDismiss: () -> Unit,
    onSave: (String) -> Unit,
    onReset: () -> Unit
) {
    var uuidText by remember { mutableStateOf(currentUuid) }
    val context = LocalContext.current
    val clipboardManager = context.getSystemService(android.content.Context.CLIPBOARD_SERVICE) as android.content.ClipboardManager

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = "⚙️ Configuración Developer",
                fontWeight = FontWeight.Bold,
                fontSize = 20.sp
            )
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Sección UUID
                Text(
                    text = "UUID para enviar al dispositivo",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )

                OutlinedTextField(
                    value = uuidText,
                    onValueChange = { uuidText = it },
                    label = { Text("UUID") },
                    placeholder = { Text("Ingresa UUID personalizado") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = false,
                    maxLines = 3
                )

                // Botones de acción rápida
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // Botón copiar
                    TextButton(
                        onClick = {
                            val clip = android.content.ClipData.newPlainText("UUID", currentUuid)
                            clipboardManager.setPrimaryClip(clip)
                        },
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("📋 Copiar", fontSize = 12.sp)
                    }

                    // Botón resetear
                    TextButton(
                        onClick = onReset,
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Refresh,
                            contentDescription = "Resetear",
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Resetear", fontSize = 12.sp)
                    }
                }

                // Info
                Text(
                    text = "💡 Puedes ingresar cualquier string. El UUID se encripta con AES-256 antes de guardarse.",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f),
                    lineHeight = 16.sp
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = { onSave(uuidText) },
                enabled = uuidText.isNotBlank()
            ) {
                Text("Guardar")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancelar")
            }
        }
    )
}
