# Documentación del Firmware ESP32 - Parking Gate

Esta guía explica cómo implementar el firmware del ESP32 para recibir comandos de la aplicación Android y controlar la pluma del estacionamiento.

## 📋 Tabla de Contenidos

- [Requisitos](#requisitos)
- [Configuración del Servicio BLE](#configuración-del-servicio-ble)
- [Recepción de Datos](#recepción-de-datos)
- [Validación del UUID](#validación-del-uuid)
- [Control de Hardware](#control-de-hardware)
- [Código de Ejemplo](#código-de-ejemplo)
- [Diagrama de Flujo](#diagrama-de-flujo)

## 🔧 Requisitos

### Hardware
- ESP32 (cualquier variante con BLE)
- Relé o módulo de control de motor
- Fuente de alimentación adecuada
- Pluma/motor del estacionamiento

### Software
- Arduino IDE 1.8+ o PlatformIO
- Biblioteca BLE de ESP32 (incluida en ESP32 Core)

### Instalación de ESP32 en Arduino IDE

1. Abrir Arduino IDE
2. Ir a `Archivo → Preferencias`
3. Agregar URL de tarjetas adicionales:
   ```
   https://raw.githubusercontent.com/espressif/arduino-esp32/gh-pages/package_esp32_index.json
   ```
4. Ir a `Herramientas → Placa → Gestor de tarjetas`
5. Buscar "ESP32" e instalar "ESP32 by Espressif Systems"

## 🔌 Configuración del Servicio BLE

### UUIDs a Utilizar

**IMPORTANTE**: Estos UUIDs deben coincidir exactamente con los de la app Android.

```cpp
// UUID del servicio BLE
#define SERVICE_UUID        "0000abcd-0000-1000-8000-00805f9b34fb"

// UUID de la característica que recibirá el UUID del usuario
#define CHARACTERISTIC_UUID "0000dcba-0000-1000-8000-00805f9b34fb"
```

### Propiedades de la Característica

La característica debe configurarse con:
- **WRITE**: Permitir que la app escriba datos
- **READ** (opcional): Permitir lectura del estado
- **NOTIFY** (opcional): Notificar cambios a la app

## 📥 Recepción de Datos

### Formato del Paquete Recibido

Cuando la app Android envía el comando, el ESP32 recibe:

```
Tipo: String UTF-8
Longitud: 36 caracteres
Formato: "xxxxxxxx-xxxx-xxxx-xxxx-xxxxxxxxxxxx"
Ejemplo: "550e8400-e29b-41d4-a716-446655440000"
```

### Callback de Escritura

El ESP32 debe implementar un callback que se ejecute cuando la app escribe en la característica:

```cpp
class MyCharacteristicCallbacks: public BLECharacteristicCallbacks {
    void onWrite(BLECharacteristic *pCharacteristic) {
        // Se ejecuta cuando la app escribe datos
        std::string value = pCharacteristic->getValue();

        if (value.length() > 0) {
            // Procesar el UUID recibido
            String userUuid = String(value.c_str());
            handleOpenCommand(userUuid);
        }
    }
};
```

## ✅ Validación del UUID

El ESP32 debe validar el UUID antes de activar la pluma:

### 1. Validación de Formato

```cpp
bool isValidUuidFormat(String uuid) {
    // Verificar longitud
    if (uuid.length() != 36) return false;

    // Verificar guiones en posiciones correctas
    if (uuid.charAt(8) != '-' || uuid.charAt(13) != '-' ||
        uuid.charAt(18) != '-' || uuid.charAt(23) != '-') {
        return false;
    }

    // Verificar que el resto sean caracteres hexadecimales
    for (int i = 0; i < uuid.length(); i++) {
        if (i == 8 || i == 13 || i == 18 || i == 23) continue;
        char c = uuid.charAt(i);
        if (!isHexadecimalDigit(c)) return false;
    }

    return true;
}
```

### 2. Validación contra Base de Datos

**Opción A: Lista Local (para pocos usuarios)**

```cpp
String authorizedUuids[] = {
    "550e8400-e29b-41d4-a716-446655440000",
    "6ba7b810-9dad-11d1-80b4-00c04fd430c8",
    "7c9e6679-7425-40de-944b-e07fc1f90ae7"
};

bool isAuthorized(String uuid) {
    int numUuids = sizeof(authorizedUuids) / sizeof(authorizedUuids[0]);
    for (int i = 0; i < numUuids; i++) {
        if (uuid.equals(authorizedUuids[i])) {
            return true;
        }
    }
    return false;
}
```

**Opción B: Servidor Remoto (para muchos usuarios)**

```cpp
bool isAuthorized(String uuid) {
    // Conectar a servidor por WiFi
    HTTPClient http;
    String url = "https://tu-servidor.com/api/validate?uuid=" + uuid;

    http.begin(url);
    int httpCode = http.GET();

    if (httpCode == 200) {
        String response = http.getString();
        http.end();
        return response.equals("valid");
    }

    http.end();
    return false;
}
```

## 🔌 Control de Hardware

### Configuración del Pin del Relé

```cpp
#define RELAY_PIN 2  // Pin GPIO para el relé
#define RELAY_ACTIVE_TIME 3000  // Tiempo activo en ms (3 segundos)

void setupHardware() {
    pinMode(RELAY_PIN, OUTPUT);
    digitalWrite(RELAY_PIN, LOW);  // Relé apagado por defecto
}
```

### Activación de la Pluma

```cpp
void activateGate() {
    Serial.println("Activando pluma del estacionamiento...");

    // Activar relé
    digitalWrite(RELAY_PIN, HIGH);

    // Esperar tiempo configurado
    delay(RELAY_ACTIVE_TIME);

    // Desactivar relé
    digitalWrite(RELAY_PIN, LOW);

    Serial.println("Pluma desactivada");
}
```

## 💻 Código de Ejemplo Completo

Ver archivo: [`esp32_parking_gate.ino`](examples/esp32_parking_gate/esp32_parking_gate.ino)

```cpp
#include <BLEDevice.h>
#include <BLEServer.h>
#include <BLEUtils.h>
#include <BLE2902.h>

// UUIDs - DEBEN coincidir con la app Android
#define SERVICE_UUID        "0000abcd-0000-1000-8000-00805f9b34fb"
#define CHARACTERISTIC_UUID "0000dcba-0000-1000-8000-00805f9b34fb"

// Configuración de hardware
#define RELAY_PIN 2
#define RELAY_ACTIVE_TIME 3000
#define LED_PIN 2  // LED integrado para feedback visual

// Variables globales
BLEServer* pServer = NULL;
BLECharacteristic* pCharacteristic = NULL;
bool deviceConnected = false;

// UUIDs autorizados (ejemplo con 3 usuarios)
String authorizedUuids[] = {
    "550e8400-e29b-41d4-a716-446655440000",
    "6ba7b810-9dad-11d1-80b4-00c04fd430c8",
    "7c9e6679-7425-40de-944b-e07fc1f90ae7"
};

/**
 * Callback para eventos de conexión del servidor BLE
 */
class MyServerCallbacks: public BLEServerCallbacks {
    void onConnect(BLEServer* pServer) {
        deviceConnected = true;
        Serial.println("Cliente conectado");
        digitalWrite(LED_PIN, HIGH);
    };

    void onDisconnect(BLEServer* pServer) {
        deviceConnected = false;
        Serial.println("Cliente desconectado");
        digitalWrite(LED_PIN, LOW);

        // Reiniciar advertising para permitir nuevas conexiones
        BLEDevice::startAdvertising();
    }
};

/**
 * Callback para eventos de escritura en la característica
 */
class MyCharacteristicCallbacks: public BLECharacteristicCallbacks {
    void onWrite(BLECharacteristic *pCharacteristic) {
        std::string value = pCharacteristic->getValue();

        if (value.length() > 0) {
            Serial.println("Datos recibidos:");
            Serial.println(value.c_str());

            // Convertir a String de Arduino
            String userUuid = String(value.c_str());

            // Procesar comando de apertura
            handleOpenCommand(userUuid);
        }
    }
};

/**
 * Valida el formato del UUID
 */
bool isValidUuidFormat(String uuid) {
    if (uuid.length() != 36) {
        Serial.println("Error: Longitud incorrecta");
        return false;
    }

    if (uuid.charAt(8) != '-' || uuid.charAt(13) != '-' ||
        uuid.charAt(18) != '-' || uuid.charAt(23) != '-') {
        Serial.println("Error: Formato de UUID incorrecto");
        return false;
    }

    return true;
}

/**
 * Verifica si el UUID está autorizado
 */
bool isAuthorized(String uuid) {
    int numUuids = sizeof(authorizedUuids) / sizeof(authorizedUuids[0]);

    for (int i = 0; i < numUuids; i++) {
        if (uuid.equals(authorizedUuids[i])) {
            Serial.println("UUID autorizado");
            return true;
        }
    }

    Serial.println("UUID NO autorizado");
    return false;
}

/**
 * Activa el relé para abrir la pluma
 */
void activateGate() {
    Serial.println("=== Activando pluma ===");

    digitalWrite(RELAY_PIN, HIGH);
    delay(RELAY_ACTIVE_TIME);
    digitalWrite(RELAY_PIN, LOW);

    Serial.println("=== Pluma cerrada ===");
}

/**
 * Maneja el comando de apertura recibido
 */
void handleOpenCommand(String userUuid) {
    Serial.println("\n--- Comando de apertura recibido ---");
    Serial.print("UUID: ");
    Serial.println(userUuid);

    // Validar formato
    if (!isValidUuidFormat(userUuid)) {
        Serial.println("RECHAZADO: Formato inválido");
        return;
    }

    // Validar autorización
    if (!isAuthorized(userUuid)) {
        Serial.println("RECHAZADO: No autorizado");
        return;
    }

    // Activar pluma
    Serial.println("ACEPTADO: Abriendo pluma");
    activateGate();
}

/**
 * Configuración inicial
 */
void setup() {
    Serial.begin(115200);
    Serial.println("\n=== ESP32 Parking Gate Controller ===");
    Serial.println("Iniciando...");

    // Configurar pines
    pinMode(RELAY_PIN, OUTPUT);
    pinMode(LED_PIN, OUTPUT);
    digitalWrite(RELAY_PIN, LOW);
    digitalWrite(LED_PIN, LOW);

    // Inicializar BLE
    BLEDevice::init("ParkingGate");

    // Crear servidor BLE
    pServer = BLEDevice::createServer();
    pServer->setCallbacks(new MyServerCallbacks());

    // Crear servicio BLE
    BLEService *pService = pServer->createService(SERVICE_UUID);

    // Crear característica BLE
    pCharacteristic = pService->createCharacteristic(
        CHARACTERISTIC_UUID,
        BLECharacteristic::PROPERTY_READ |
        BLECharacteristic::PROPERTY_WRITE
    );

    pCharacteristic->setCallbacks(new MyCharacteristicCallbacks());
    pCharacteristic->addDescriptor(new BLE2902());

    // Iniciar servicio
    pService->start();

    // Iniciar advertising
    BLEAdvertising *pAdvertising = BLEDevice::getAdvertising();
    pAdvertising->addServiceUUID(SERVICE_UUID);
    pAdvertising->setScanResponse(true);
    pAdvertising->setMinPreferred(0x06);
    pAdvertising->setMinPreferred(0x12);
    BLEDevice::startAdvertising();

    Serial.println("Servidor BLE iniciado");
    Serial.println("Esperando conexiones...");
}

/**
 * Loop principal
 */
void loop() {
    // El trabajo se hace en los callbacks
    delay(1000);

    // Opcional: Imprimir estado cada 10 segundos
    static unsigned long lastPrint = 0;
    if (millis() - lastPrint > 10000) {
        lastPrint = millis();
        Serial.print("Estado: ");
        Serial.println(deviceConnected ? "Conectado" : "Esperando...");
    }
}
```

## 📊 Diagrama de Flujo

```
┌─────────────────────┐
│   ESP32 Inicia      │
│   Servidor BLE      │
└──────────┬──────────┘
           │
           ▼
┌─────────────────────┐
│   Anuncia Servicio  │
│   SERVICE_UUID      │
└──────────┬──────────┘
           │
           ▼
┌─────────────────────┐
│  Espera Conexión    │◄─────────┐
│  de App Android     │          │
└──────────┬──────────┘          │
           │                      │
           ▼                      │
┌─────────────────────┐          │
│  App Conecta        │          │
│  vía BLE            │          │
└──────────┬──────────┘          │
           │                      │
           ▼                      │
┌─────────────────────┐          │
│  Espera Escritura   │          │
│  en Característica  │          │
└──────────┬──────────┘          │
           │                      │
           ▼                      │
┌─────────────────────┐          │
│  Recibe UUID        │          │
│  del Usuario        │          │
└──────────┬──────────┘          │
           │                      │
           ▼                      │
┌─────────────────────┐          │
│  Valida Formato     │          │
│  UUID               │          │
└──────────┬──────────┘          │
           │                      │
        ¿Válido?                 │
           │                      │
     No────┴──────► Rechazar     │
           │                      │
        Sí │                      │
           ▼                      │
┌─────────────────────┐          │
│  Verifica si UUID   │          │
│  está Autorizado    │          │
└──────────┬──────────┘          │
           │                      │
    ¿Autorizado?                 │
           │                      │
     No────┴──────► Rechazar     │
           │                      │
        Sí │                      │
           ▼                      │
┌─────────────────────┐          │
│  Activa Relé        │          │
│  (Abre Pluma)       │          │
└──────────┬──────────┘          │
           │                      │
           ▼                      │
┌─────────────────────┐          │
│  Espera 3 seg       │          │
└──────────┬──────────┘          │
           │                      │
           ▼                      │
┌─────────────────────┐          │
│  Desactiva Relé     │          │
│  (Cierra Pluma)     │          │
└──────────┬──────────┘          │
           │                      │
           │                      │
           └──────────────────────┘
```

## 🔍 Debugging

### Monitor Serial

El código de ejemplo imprime información detallada por serial:

```
=== ESP32 Parking Gate Controller ===
Iniciando...
Servidor BLE iniciado
Esperando conexiones...
Cliente conectado
Datos recibidos:
550e8400-e29b-41d4-a716-446655440000

--- Comando de apertura recibido ---
UUID: 550e8400-e29b-41d4-a716-446655440000
UUID autorizado
ACEPTADO: Abriendo pluma
=== Activando pluma ===
=== Pluma cerrada ===
```

### Solución de Problemas Comunes

**La app no encuentra el ESP32**
- Verifica que el `SERVICE_UUID` coincida exactamente
- Confirma que el ESP32 esté ejecutando `BLEDevice::startAdvertising()`
- Verifica alimentación del ESP32

**El ESP32 recibe datos pero no activa el relé**
- Verifica el UUID recibido por serial
- Confirma que el UUID está en la lista `authorizedUuids[]`
- Verifica conexión del relé al `RELAY_PIN`

## 🔐 Consideraciones de Seguridad

1. **No hardcodear UUIDs sensibles** - Usa almacenamiento seguro o servidor
2. **Implementa rate limiting** - Evita spam de comandos
3. **Registra intentos fallidos** - Para detección de intrusos
4. **Usa encriptación** - Si manejas datos sensibles
5. **Actualiza firmware regularmente** - Para parches de seguridad

## 📚 Recursos Adicionales

- [Documentación oficial ESP32 BLE](https://docs.espressif.com/projects/esp-idf/en/latest/esp32/api-reference/bluetooth/index.html)
- [Especificación Bluetooth GATT](https://www.bluetooth.com/specifications/gatt/)
- [Arduino ESP32 GitHub](https://github.com/espressif/arduino-esp32)

## 📧 Soporte

Para preguntas sobre el firmware del ESP32, consulta la documentación principal o abre un issue en el repositorio.

---

**Nota**: Esta documentación asume conocimientos básicos de programación de ESP32 y Arduino. Para información sobre la app Android, consulta [README.md](README.md).
