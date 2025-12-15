# Bluetooth Dev App - Android BLE Controller

> Aplicación Android profesional en Kotlin para controlar dispositivos ESP32 mediante Bluetooth Low Energy (BLE)

[![Kotlin](https://img.shields.io/badge/Kotlin-2.0.20-blue.svg)](https://kotlinlang.org)
[![Android](https://img.shields.io/badge/Android-API%2021%2B-green.svg)](https://developer.android.com)
[![Compose](https://img.shields.io/badge/Jetpack%20Compose-1.7-orange.svg)](https://developer.android.com/jetpack/compose)
[![License](https://img.shields.io/badge/License-MIT-yellow.svg)](LICENSE)

---

## Tabla de Contenidos

- [Descripción](#descripción)
- [Características](#características)
- [Arquitectura](#arquitectura)
- [Tecnologías](#tecnologías)
- [Requisitos](#requisitos)
- [Instalación](#instalación)
- [Uso](#uso)
- [Configuración Developer](#configuración-developer)
- [Protocolo BLE](#protocolo-ble)
- [Seguridad](#seguridad)
- [Solución de Problemas](#solución-de-problemas)
- [Roadmap](#roadmap)
- [Contribución](#contribución)
- [Licencia](#licencia)

---

## Descripción

**Bluetooth Dev App** es una aplicación Android nativa desarrollada en Kotlin que permite controlar dispositivos ESP32 de forma inalámbrica mediante Bluetooth Low Energy (BLE). Diseñada originalmente para controlar plumas de estacionamiento automáticas, la app es completamente configurable para cualquier proyecto que requiera comunicación BLE con ESP32.

### Casos de Uso

- Control de acceso automático (plumas, portones)
- Domótica y automatización del hogar
- Proyectos IoT con ESP32
- Prototipado rápido de comunicación BLE
- Herramienta para developers que trabajan con ESP32

---

## Características

### Core Features

- **Detección Automática** - Escanea y encuentra dispositivos ESP32 cercanos
- **Conexión Sin Pairing** - No requiere emparejamiento Bluetooth manual
- **UUID Único por Dispositivo** - Identificador seguro y persistente
- **Comunicación Ultra-Rápida** - Latencia mínima optimizada
- **Auto-Desconexión** - Se desconecta automáticamente tras completar la operación

### Características Avanzadas

#### Experiencia de Usuario
- **Tema Oscuro/Claro** - Modo oscuro con toggle dinámico
- **Feedback Háptico** - Vibraciones contextuales para cada acción
- **Animaciones Fluidas** - Material Design 3 con spring animations
- **Indicador RSSI** - Muestra intensidad de señal en tiempo real
- **Log en Tiempo Real** - Visualiza todas las transacciones BLE

#### Confiabilidad
- **Auto-Reconexión** - Sistema inteligente con exponential backoff
- **Manejo de Errores** - Feedback claro con opción de reintentar
- **Timeouts Configurables** - Previene bloqueos indefinidos
- **Estado de Batería** - Optimizado para bajo consumo

#### Developer Tools
- **UUID Editable** - Modifica el UUID sin recompilar
- **Debug Mode** - Exporta logs en JSON/TXT
- **Historial de Conexiones** - Base de datos local con Room
- **Información del Sistema** - Detalles del dispositivo y app

#### Seguridad
- **Encriptación AES-256-GCM** - UUID encriptado en almacenamiento
- **Android KeyStore** - Claves protegidas por hardware
- **Sin Datos Sensibles** - No almacena información personal

---

## Arquitectura

La aplicación sigue el patrón **MVVM (Model-View-ViewModel)** con **Clean Architecture**:

```
┌─────────────────────────────────────────────────────────────┐
│                  UI LAYER (Jetpack Compose)                 │
│                                                             │
│  MainActivity → MainScreen → Components                    │
│    ↓ Observa StateFlow                                     │
└─────────────────────────┬───────────────────────────────────┘
                          │
┌─────────────────────────▼───────────────────────────────────┐
│               PRESENTATION LAYER (ViewModel)                │
│                                                             │
│  MainViewModel - Coordina lógica de negocio                │
│    - connectionState: StateFlow<BleConnectionState>        │
│    - userUuid: StateFlow<String?>                          │
│    - bleLog: StateFlow<List<String>>                       │
│    - isDarkTheme: Flow<Boolean>                            │
└─────────────────────────┬───────────────────────────────────┘
                          │
┌─────────────────────────▼───────────────────────────────────┐
│                    DATA LAYER                               │
│                                                             │
│  ┌──────────────────┐  ┌──────────────────┐  ┌──────────┐ │
│  │   BleManager     │  │ UserRepository   │  │  Room DB │ │
│  │                  │  │                  │  │          │ │
│  │ - Comunicación   │  │ - UUID Storage   │  │ - History│ │
│  │ - Auto-reconexión│  │ - Encryption     │  │ - Stats  │ │
│  │ - RSSI tracking  │  │ - DataStore      │  │          │ │
│  └──────────────────┘  └──────────────────┘  └──────────┘ │
└─────────────────────────────────────────────────────────────┘
```

### Flujo de Operación

1. **Usuario presiona "ABRIR"**
2. `MainViewModel.onOpenButtonClick()` coordina la operación
3. `BleManager` escanea dispositivos con filtro UUID
4. Encuentra y conecta al ESP32 automáticamente
5. Descubre servicios GATT y características
6. Envía UUID del usuario (36 bytes UTF-8)
7. ESP32 valida UUID y ejecuta acción
8. App se desconecta automáticamente
9. Guarda registro en historial local

### Estados de Conexión

```kotlin
sealed class BleConnectionState {
    object Idle         // Esperando acción del usuario
    object Scanning     // Buscando dispositivo ESP32
    object Connecting   // Estableciendo conexión GATT
    object Connected    // Conexión establecida, servicios descubiertos
    object Opening      // Enviando comando UUID
    object Success      // Operación completada exitosamente
    data class Error(
        val message: String,
        val canRetry: Boolean
    )
}
```

---

## Tecnologías

### Android/Kotlin
- **Kotlin 2.0.20** - Lenguaje moderno y conciso
- **Jetpack Compose** - UI declarativa y reactiva
- **Material Design 3** - Sistema de diseño moderno
- **Kotlin Coroutines** - Programación asíncrona estructurada
- **StateFlow/Flow** - Manejo reactivo de estado

### Android Jetpack
- **ViewModel** - Gestión de estado con ciclo de vida
- **DataStore** - Almacenamiento key-value asíncrono
- **Room Database** - Base de datos SQL local
- **Lifecycle** - Componentes lifecycle-aware

### Bluetooth
- **Android BLE APIs** - BluetoothAdapter, BluetoothGatt
- **GATT Protocol** - Generic Attribute Profile
- **Service Discovery** - Descubrimiento automático

### Seguridad
- **Android KeyStore** - Almacenamiento seguro de claves
- **AES-256-GCM** - Encriptación simétrica
- **IV (Initialization Vector)** - Seguridad adicional

### Build & Tools
- **Gradle 8.2+** - Sistema de build
- **KSP (Kotlin Symbol Processing)** - Generación de código
- **Compose Compiler** - Compilador de Compose

---

## Requisitos

### Dispositivo Android
- **Android 5.0 (API 21) o superior**
- **Soporte Bluetooth Low Energy (BLE)**
- **4.0+ recomendado para mejor experiencia**

### Permisos

#### Android 12+ (API 31+)
```xml
<uses-permission android:name="android.permission.BLUETOOTH_SCAN"
    android:usesPermissionFlags="neverForLocation" />
<uses-permission android:name="android.permission.BLUETOOTH_CONNECT" />
<uses-permission android:name="android.permission.VIBRATE" />
```

#### Android 11 y anteriores
```xml
<uses-permission android:name="android.permission.BLUETOOTH" />
<uses-permission android:name="android.permission.BLUETOOTH_ADMIN" />
<uses-permission android:name="android.permission.ACCESS_FINE_LOCATION" />
<uses-permission android:name="android.permission.VIBRATE" />
```

### Hardware ESP32
- Cualquier ESP32 con soporte BLE
- Firmware configurado con UUIDs correctos
- Ver [README_ESP32.md](README_ESP32.md) para detalles

---

## Instalación

### Opción 1: Compilar desde Código Fuente

#### Requisitos de Desarrollo
- **Android Studio Hedgehog (2023.1.1) o superior**
- **JDK 17**
- **Gradle 8.2+**
- **Android SDK API 34**

#### Pasos

1. **Clonar el repositorio**
```bash
git clone <repository-url>
cd ejemplo_BLE_kigo
```

2. **Abrir en Android Studio**
```
File → Open → Seleccionar la carpeta del proyecto
```

3. **Sincronizar Gradle**
```
Android Studio ejecutará gradle sync automáticamente
```

4. **Compilar**
```bash
# Desde terminal
./gradlew assembleDebug

# O usar el botón "Run" en Android Studio
```

5. **Instalar en dispositivo**
```bash
# Via ADB
./gradlew installDebug

# O desde Android Studio: Run → Run 'app'
```

### Opción 2: Instalar APK Pre-compilado

1. Descarga el APK desde [Releases]()
2. Habilita "Instalar apps desconocidas" en tu Android
3. Instala el APK
4. Concede permisos Bluetooth al primer uso

### Compilación desde Terminal (Sin Android Studio)

#### macOS/Linux
```bash
# Compilar debug APK
./gradlew assembleDebug

# Instalar via ADB
adb install -r app/build/outputs/apk/debug/app-debug.apk

# Compilar release APK (firmado)
./gradlew assembleRelease
```

#### Windows
```cmd
gradlew.bat assembleDebug
adb install -r app\build\outputs\apk\debug\app-debug.apk
```

---

## Uso

### Primera Vez

1. **Abrir la aplicación**
   - La app solicitará permisos Bluetooth
   - Conceder todos los permisos necesarios

2. **Generar UUID**
   - Al primer uso, se genera un UUID único automáticamente
   - Se almacena encriptado con AES-256-GCM
   - Persiste entre reinicios de la app

3. **Encender ESP32**
   - Asegúrate que tu ESP32 esté encendido
   - Debe estar anunciando el servicio BLE correcto
   - Rango: ~10 metros (depende del entorno)

### Operación Normal

1. **Presionar botón "ABRIR"**
   - El botón naranja central inicia la operación
   - Se deshabilita durante el proceso

2. **Observar estados**
   - **Escaneando dispositivo...** - Buscando ESP32
   - **Conectando...** - Estableciendo conexión
   - **Conectado** - Servicios descubiertos
   - **Abriendo pluma...** - Enviando comando
   - **¡Listo!** - Operación completada

3. **Log en tiempo real**
   - Scroll hacia abajo para ver transacciones BLE
   - Muestra timestamps y eventos detallados

### Características Adicionales

#### Cambiar Tema
- Presiona el ícono de sol/luna en la esquina superior derecha
- Alterna entre modo claro y oscuro
- Preferencia guardada persistentemente

#### Reintentar Tras Error
- Si ocurre un error, aparece botón "Reintentar"
- Presiona para intentar la operación nuevamente
- El estado se reinicia a Idle

#### Ver Señal RSSI
- Indicador visual muestra intensidad de señal
- Verde: Señal excelente (> -70 dBm)
- Amarillo: Señal buena (-70 a -85 dBm)
- Rojo: Señal débil (< -85 dBm)

---

## Configuración Developer

La app incluye un panel de configuración completo para developers que necesitan modificar parámetros sin recompilar.

### Acceder a Configuración

1. Presiona el ícono de **Configuración** (⚙️) en la esquina superior izquierda
2. Se abre el diálogo "Configuración Developer"

### Modificar UUID

#### ¿Por qué modificar el UUID?

Durante desarrollo, es útil poder cambiar el UUID enviado al ESP32 para:
- Probar diferentes usuarios
- Debugging de validación en ESP32
- Testing de autorización
- Desarrollo sin ESP32 real

#### Pasos

1. **Copiar UUID Actual**
   - Presiona botón "📋 Copiar"
   - UUID se copia al portapapeles

2. **Editar UUID**
   - Modifica el texto en el campo
   - Puede ser cualquier string (no solo formato UUID)
   - Máximo 3 líneas

3. **Guardar**
   - Presiona "Guardar"
   - UUID se encripta y guarda con AES-256-GCM
   - Disponible inmediatamente para próximas conexiones

4. **Resetear a Autogenerado**
   - Presiona botón "Resetear"
   - Genera nuevo UUID aleatorio
   - Útil para volver al estado original

### Exportar Logs de Debug

#### Desde la UI
1. Presiona botón "Exportar Logs" (si está visible)
2. Selecciona formato (JSON o TXT)
3. Los logs se exportan con:
   - Timestamps precisos
   - Información del sistema
   - Todos los eventos BLE
   - Metadatos de la app

#### Formato JSON
```json
{
  "export_info": {
    "timestamp": "2025-12-15T10:30:00Z",
    "app_version": "1.0.0",
    "format": "json"
  },
  "system_info": {
    "device_model": "Pixel 6",
    "android_version": "14",
    "sdk_int": 34,
    "manufacturer": "Google"
  },
  "logs": [
    {
      "timestamp": "10:30:45.123",
      "message": "Iniciando escaneo BLE..."
    }
  ]
}
```

#### Formato TXT
```
=== BLE Logs Export ===
Fecha: 2025-12-15 10:30:00
App Version: 1.0.0

--- System Info ---
Device: Google Pixel 6
Android: 14 (API 34)

--- Logs ---
[10:30:45.123] Iniciando escaneo BLE...
[10:30:45.456] Dispositivo encontrado: ESP32_Gate
```

### Ver Historial de Conexiones

La app guarda automáticamente todas las conexiones en una base de datos local Room.

#### Información Registrada
- Timestamp de conexión
- Nombre del dispositivo
- Dirección MAC
- RSSI (señal)
- Resultado (SUCCESS/ERROR)
- Mensaje
- UUID utilizado
- Duración total

#### Estadísticas
- Total de conexiones
- Conexiones exitosas
- Tasa de éxito
- Últimos 20 registros

---

## Protocolo BLE

### UUIDs del Servicio

```kotlin
// Definidos en Constants.kt
SERVICE_UUID = "0000abcd-0000-1000-8000-00805f9b34fb"
CHARACTERISTIC_UUID = "0000dcba-0000-1000-8000-00805f9b34fb"
```

**IMPORTANTE**: Estos UUIDs deben coincidir exactamente con los configurados en el ESP32.

### Flujo de Comunicación Detallado

```
1. SCAN
   ├─ ScanFilter con SERVICE_UUID
   ├─ SCAN_MODE_LOW_LATENCY para velocidad
   ├─ Timeout: 10 segundos
   └─ Callback: onScanResult()

2. CONNECT
   ├─ device.connectGatt(context, autoConnect=false, callback)
   ├─ TRANSPORT_LE (Low Energy)
   └─ Callback: onConnectionStateChange()

3. DISCOVER SERVICES
   ├─ gatt.discoverServices()
   └─ Callback: onServicesDiscovered()

4. GET CHARACTERISTIC
   ├─ service = gatt.getService(SERVICE_UUID)
   ├─ characteristic = service.getCharacteristic(CHARACTERISTIC_UUID)
   └─ Verifica PROPERTY_WRITE

5. WRITE UUID
   ├─ characteristic.setValue(uuid.toByteArray(Charsets.UTF_8))
   ├─ gatt.writeCharacteristic(characteristic)
   └─ Callback: onCharacteristicWrite()

6. DISCONNECT
   ├─ delay(1500ms) para que ESP32 procese
   ├─ gatt.disconnect()
   └─ gatt.close()
```

### Formato del Paquete

#### UUID Enviado
```
Tipo: String UTF-8
Longitud: 36 caracteres
Formato: "xxxxxxxx-xxxx-xxxx-xxxx-xxxxxxxxxxxx"
Ejemplo: "550e8400-e29b-41d4-a716-446655440000"
Tamaño: 36 bytes
```

#### Características de la Característica BLE
```
Properties: WRITE
Permissions: WRITE
Max Size: 512 bytes (BLE estándar)
Write Type: WRITE_TYPE_DEFAULT (con respuesta)
```

### Timeouts y Configuración

```kotlin
// Constants.kt
const val SCAN_TIMEOUT_MS = 10000L       // 10 segundos
const val CONNECTION_TIMEOUT_MS = 5000L   // 5 segundos
const val WRITE_TIMEOUT_MS = 3000L        // 3 segundos
const val AUTO_RECONNECT_MAX_RETRIES = 3  // 3 intentos
const val AUTO_RECONNECT_BASE_DELAY = 1000L // 1 segundo base
```

### Auto-Reconexión

Sistema de reconexión inteligente con **exponential backoff**:

```
Intento 1: Espera 1 segundo
Intento 2: Espera 2 segundos (1 * 2)
Intento 3: Espera 4 segundos (2 * 2)
Intento 4: Falla y reporta error
```

**Desactivación temporal**: Cuando se desconecta intencionalmente tras éxito, el sistema temporalmente desactiva auto-reconexión para evitar loops.

---

## Seguridad

### Encriptación de Datos

#### UUID Storage
- **Algoritmo**: AES-256-GCM (Galois/Counter Mode)
- **Almacenamiento**: Android KeyStore (hardware-backed)
- **IV**: Initialization Vector único por encriptación
- **Formato**: `[IV_Base64]CipherText_Base64`

```kotlin
// Ejemplo de encriptación
plainText: "550e8400-e29b-41d4-a716-446655440000"
encrypted: "xK8pL3...IV...]R4tN9...ciphertext..."
```

#### KeyStore
```kotlin
KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, "AndroidKeyStore")
keyGenParameterSpec {
    purposes = KeyProperties.PURPOSE_ENCRYPT | KeyProperties.PURPOSE_DECRYPT
    blockModes = KeyProperties.BLOCK_MODE_GCM
    encryptionPaddings = KeyProperties.ENCRYPTION_PADDING_NONE
    keySize = 256
}
```

### Comunicación BLE

#### Seguridad Inherente
- **Rango limitado**: ~10 metros (dificulta ataques remotos)
- **No pairing**: Reduce superficie de ataque
- **Conexión efímera**: Duración mínima de conexión

#### Validación en ESP32
El ESP32 **DEBE** implementar:
1. Validación de formato UUID
2. Lista blanca de UUIDs autorizados
3. Rate limiting
4. Logging de intentos fallidos

Ver [README_ESP32.md](README_ESP32.md) para implementación completa.

### Mejores Prácticas

#### Lo que la app HACE
- Encripta UUID en reposo
- No almacena datos personales
- No requiere cuenta de usuario
- No envía datos a servidores externos
- Minimiza permisos solicitados

#### Lo que la app NO HACE
- No encripta comunicación BLE (estándar BLE)
- No implementa autenticación de usuario
- No valida autorización (responsabilidad del ESP32)

### Recomendaciones

#### Para Producción
1. **Autenticación de usuario** - Agregar login
2. **Servidor de validación** - UUID checks remotos
3. **Certificados** - Para comunicación HTTPS si se agrega backend
4. **Ofuscación de código** - ProGuard/R8 en release
5. **Biometría** - Para acceso a la app

#### Para Desarrollo
- No uses UUID reales en logs públicos
- Limita quién tiene acceso al ESP32
- Mantén firmware ESP32 actualizado

---

## Solución de Problemas

### La app no encuentra el dispositivo

#### Síntomas
- Estado permanece en "Escaneando dispositivo..."
- Timeout después de 10 segundos
- Error: "Dispositivo no encontrado"

#### Soluciones

1. **Verifica el ESP32**
   ```
   - ¿Está encendido?
   - ¿LED parpadeando? (indica advertising)
   - ¿Puerto serial muestra "Esperando conexiones..."?
   ```

2. **Verifica UUIDs**
   ```kotlin
   // App (Constants.kt)
   SERVICE_UUID = "0000abcd-0000-1000-8000-00805f9b34fb"

   // ESP32 (sketch)
   #define SERVICE_UUID "0000abcd-0000-1000-8000-00805f9b34fb"
   ```
   Deben ser **idénticos**.

3. **Verifica Bluetooth**
   - Android: Configuración → Bluetooth → Activado
   - Otras apps BLE: Ciérralas (pueden interferir)
   - Reinicia Bluetooth: Off → On

4. **Rango**
   - Acércate a menos de 5 metros
   - Evita obstáculos metálicos
   - Intenta en espacio abierto

5. **Reinicia**
   - Reinicia la app
   - Reinicia el ESP32
   - Reinicia Bluetooth del teléfono

### Error de conexión

#### Síntomas
- "Error: Connection failed (status 133)"
- "Error: Connection timeout"
- Se conecta pero desconecta inmediatamente

#### Soluciones

1. **Error 133** (GATT_ERROR)
   ```
   - Causa: ESP32 no responde correctamente
   - Solución: Reinicia ESP32 completamente (reset físico)
   ```

2. **Conexión Inestable**
   ```
   - Batería baja en teléfono o ESP32
   - Interferencia WiFi (cambia canal ESP32)
   - Demasiados dispositivos BLE cercanos
   ```

3. **Conexión Múltiple**
   ```
   - Solo un dispositivo puede conectar al ESP32 a la vez
   - Desconecta otros dispositivos primero
   ```

### Permisos denegados

#### Android 12+
```
Configuración → Apps → Bluetooth Dev App → Permisos
  - Bluetooth: Permitir
  - Ubicación cercana: Permitir (o "Solo mientras uso la app")
```

#### Android 11 o menor
```
Configuración → Apps → Bluetooth Dev App → Permisos
  - Ubicación: Permitir
  - Desinstala y reinstala si es necesario
```

### UUID no se envía

#### Verificar en Logs
```
[14:30:45.123] Conectado - Servicios descubiertos
[14:30:45.456] Enviando UUID: 550e8400...
[14:30:45.789] ✓ UUID enviado correctamente al ESP32
```

Si no ves "UUID enviado":
1. **Característica no encontrada**
   - Verifica CHARACTERISTIC_UUID en app y ESP32
2. **Sin permisos de escritura**
   - ESP32: characteristic debe tener PROPERTY_WRITE

#### Verificar en ESP32
```cpp
void onWrite(BLECharacteristic *pCharacteristic) {
    Serial.print("Datos recibidos: ");
    Serial.println(pCharacteristic->getValue().c_str());
}
```

Si ESP32 no recibe nada:
- Verifica que callback esté registrado
- Verifica que característica sea la correcta

### App se cierra inesperadamente

#### Recolectar Logs
```bash
# Via ADB
adb logcat -d AndroidRuntime:E *:F > error_log.txt

# Compartir con desarrolladores
```

#### Causas Comunes
1. **Falta de permisos** - Concede todos los requeridos
2. **Android antiguo** - Requiere API 21+ (Android 5.0+)
3. **Sin BLE** - Dispositivo no compatible con Bluetooth LE

### Problemas de Performance

#### App lenta
```
- Cierra otras apps en segundo plano
- Limpia caché: Configuración → Storage → Limpiar caché
- Verifica RAM disponible (necesita ~50MB)
```

#### Batería se agota rápido
```
- Normal durante uso intensivo BLE
- Desactiva auto-reconexión si no se usa
- Cierra la app cuando no la uses
```

---

## Contribución

¡Las contribuciones son bienvenidas! Por favor:

1. **Fork** el repositorio
2. **Crea** una rama para tu feature (`git checkout -b feature/AmazingFeature`)
3. **Commit** tus cambios (`git commit -m 'Add some AmazingFeature'`)
4. **Push** a la rama (`git push origin feature/AmazingFeature`)
5. **Abre** un Pull Request

### Guidelines

- Sigue las convenciones de Kotlin
- Documenta código con KDoc
- Agrega tests cuando sea posible
- Actualiza README si es necesario
- Mantén commits atómicos y descriptivos

### Código de Conducta

Sé respetuoso, inclusivo y profesional. Ver [CODE_OF_CONDUCT.md](CODE_OF_CONDUCT.md).

---

## Documentación Adicional

- **[README_ESP32.md](README_ESP32.md)** - Guía completa del firmware ESP32
- **[Código Fuente](app/src/main/java/com/parkingate/controller/)** - Comentarios detallados en el código

---

## Licencia

Este proyecto está bajo la licencia MIT. Ver [LICENSE](LICENSE) para más detalles.

```
MIT License

Copyright (c) 2025 [Tu Nombre]

Permission is hereby granted, free of charge, to any person obtaining a copy
of this software and associated documentation files (the "Software"), to deal
in the Software without restriction...
```

---

## Contacto

**Developer**: [Tu Nombre]
**Email**: [tu@email.com]
**GitHub**: [@tuusuario](https://github.com/tuusuario)
**Issues**: [GitHub Issues](https://github.com/tuusuario/bluetooth-dev-app/issues)

---

## Reconocimientos

- **Jetpack Compose** - Framework UI moderno
- **Android BLE APIs** - Comunicación Bluetooth
- **Material Design** - Sistema de diseño
- **ESP32 Community** - Soporte y recursos
- **Contributors** - Gracias a todos los colaboradores

---

## Notas de Versión

### v1.0.0 - Release Inicial (2025-12-15)

#### Features Principales
- ✅ Comunicación BLE con ESP32
- ✅ UUID único por dispositivo
- ✅ Auto-reconexión inteligente
- ✅ Tema oscuro/claro
- ✅ Feedback háptico
- ✅ Indicador RSSI
- ✅ Log de transacciones BLE
- ✅ Historial de conexiones (Room DB)
- ✅ Encriptación AES-256-GCM
- ✅ Configuración developer (UUID editable)
- ✅ Exportación de logs (JSON/TXT)
- ✅ Auto-desconexión tras éxito

#### Bug Fixes
- 🐛 UUID se envía correctamente en UTF-8
- 🐛 Desconexión automática tras operación exitosa
- 🐛 Auto-reconexión no interfiere con desconexión intencional
- 🐛 Permisos Android 12+ manejados correctamente

#### Mejoras de Performance
- ⚡ Escaneo optimizado con filtros
- ⚡ Timeouts configurables
- ⚡ StateFlow en lugar de LiveData
- ⚡ Compose con skip optimizations

---

<div align="center">

**[⬆ Volver arriba](#bluetooth-dev-app---android-ble-controller)**

Hecho con ❤️ y Kotlin

</div>
