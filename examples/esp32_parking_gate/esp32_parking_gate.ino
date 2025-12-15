/*
 * ESP32 Parking Gate Controller
 *
 * Este firmware permite que un ESP32 reciba comandos de una aplicación Android
 * mediante BLE (Bluetooth Low Energy) para controlar una pluma de estacionamiento.
 *
 * Autor: [Tu nombre]
 * Fecha: 2024
 * Versión: 2.0 - Compatible con app Android ParkingGate Controller
 *
 * Hardware requerido:
 * - ESP32 (cualquier variante con BLE)
 * - Módulo relé conectado al GPIO 2
 * - Pluma/motor del estacionamiento
 *
 * Características:
 * - Servidor BLE con servicio personalizado
 * - Recepción de UUID de usuario
 * - Validación de formato y autorización
 * - Control de relé para activar pluma
 * - Feedback por LED y Serial Monitor
 * - Respuesta BLE al cliente (OK/DENIED/ERROR)
 * - Modo TEST para pruebas sin autorización
 *
 * MODO TEST:
 * - Cuando TEST_MODE está en true, acepta CUALQUIER UUID válido
 * - Útil para probar la app sin configurar UUIDs autorizados
 * - El UUID recibido se muestra en Serial para poder agregarlo a la lista
 */

#include <BLEDevice.h>
#include <BLEServer.h>
#include <BLEUtils.h>
#include <BLE2902.h>

// ============================================================================
// CONFIGURACIÓN - Modifica estos valores según tus necesidades
// ============================================================================

// *** MODO TEST ***
// Cambia a 'true' para probar la app sin verificar autorización
// En modo TEST, cualquier UUID con formato válido abrirá la pluma
// IMPORTANTE: Pon en 'false' para producción
#define TEST_MODE true

// UUIDs del servicio y característica BLE
// IMPORTANTE: Deben coincidir EXACTAMENTE con los de la app Android
#define SERVICE_UUID        "0000abcd-0000-1000-8000-00805f9b34fb"
#define CHARACTERISTIC_UUID "0000dcba-0000-1000-8000-00805f9b34fb"

// Nombre del dispositivo BLE (visible al escanear)
#define DEVICE_NAME "ParkingGate"

// Configuración de hardware
#define RELAY_PIN 2           // Pin GPIO para controlar el relé
#define RELAY_ACTIVE_TIME 3000 // Tiempo que el relé permanece activo (ms)
#define LED_PIN 22             // LED integrado para feedback visual

// UUIDs autorizados para abrir la pluma
// Agrega aquí los UUIDs generados por las apps Android autorizadas
// TIP: El UUID de cada dispositivo Android se muestra en Serial cuando se conecta
String authorizedUuids[] = {
    "550e8400-e29b-41d4-a716-446655440000",
    "6ba7b810-9dad-11d1-80b4-00c04fd430c8",
    "7c9e6679-7425-40de-944b-e07fc1f90ae7"
    // Agrega más UUIDs aquí separados por comas
};

// ============================================================================
// VARIABLES GLOBALES
// ============================================================================

BLEServer* pServer = NULL;
BLECharacteristic* pCharacteristic = NULL;
bool deviceConnected = false;
bool oldDeviceConnected = false;

// ============================================================================
// FORWARD DECLARATIONS
// ============================================================================

void handleOpenCommand(String userUuid);

// ============================================================================
// CALLBACKS BLE
// ============================================================================

/**
 * Callback para manejar eventos de conexión/desconexión del servidor BLE.
 *
 * Eventos manejados:
 * - onConnect: Cuando un cliente (app Android) se conecta
 * - onDisconnect: Cuando un cliente se desconecta
 */
class MyServerCallbacks: public BLEServerCallbacks {
    void onConnect(BLEServer* pServer) {
        deviceConnected = true;
        Serial.println("\n[INFO] Cliente BLE conectado");
        digitalWrite(LED_PIN, HIGH);
    };

    void onDisconnect(BLEServer* pServer) {
        deviceConnected = false;
        Serial.println("[INFO] Cliente BLE desconectado");
        digitalWrite(LED_PIN, LOW);

        // Reiniciar advertising para permitir nuevas conexiones
        Serial.println("[INFO] Reiniciando advertising...");
        BLEDevice::startAdvertising();
    }
};

/**
 * Callback para manejar eventos de escritura en la característica BLE.
 *
 * Este callback se ejecuta cuando la app Android escribe datos en la
 * característica CHARACTERISTIC_UUID. Los datos recibidos contienen
 * el UUID único del usuario que intenta abrir la pluma.
 */
class MyCharacteristicCallbacks: public BLECharacteristicCallbacks {
    void onWrite(BLECharacteristic *pCharacteristic) {
        // Obtener el valor escrito por la app
        String value = pCharacteristic->getValue();

        if (value.length() > 0) {
            Serial.println("\n[BLE] Datos recibidos de la app:");
            Serial.print("[BLE] Longitud: ");
            Serial.print(value.length());
            Serial.println(" bytes");
            Serial.print("[BLE] Contenido: ");
            Serial.println(value);

            // Procesar el comando de apertura
            handleOpenCommand(value);
        } else {
            Serial.println("[WARNING] Datos vacíos recibidos");
        }
    }
};

// ============================================================================
// FUNCIONES DE VALIDACIÓN
// ============================================================================

/**
 * Valida que el UUID tenga el formato correcto.
 *
 * Formato esperado: "xxxxxxxx-xxxx-xxxx-xxxx-xxxxxxxxxxxx"
 * - 8 caracteres hex
 * - guión
 * - 4 caracteres hex
 * - guión
 * - 4 caracteres hex
 * - guión
 * - 4 caracteres hex
 * - guión
 * - 12 caracteres hex
 *
 * @param uuid String con el UUID a validar
 * @return true si el formato es válido, false en caso contrario
 */
bool isValidUuidFormat(String uuid) {
    // Verificar longitud total (36 caracteres)
    if (uuid.length() != 36) {
        Serial.println("[ERROR] Longitud incorrecta de UUID");
        Serial.print("[ERROR] Esperado: 36, Recibido: ");
        Serial.println(uuid.length());
        return false;
    }

    // Verificar guiones en posiciones correctas
    if (uuid.charAt(8) != '-' || uuid.charAt(13) != '-' ||
        uuid.charAt(18) != '-' || uuid.charAt(23) != '-') {
        Serial.println("[ERROR] Formato de UUID incorrecto (guiones mal ubicados)");
        return false;
    }

    // Verificar que el resto sean caracteres hexadecimales válidos
    for (int i = 0; i < uuid.length(); i++) {
        // Saltar guiones
        if (i == 8 || i == 13 || i == 18 || i == 23) continue;

        char c = uuid.charAt(i);
        if (!isHexadecimalDigit(c)) {
            Serial.print("[ERROR] Carácter no hexadecimal encontrado en posición ");
            Serial.print(i);
            Serial.print(": '");
            Serial.print(c);
            Serial.println("'");
            return false;
        }
    }

    Serial.println("[OK] Formato de UUID válido");
    return true;
}

/**
 * Verifica si el UUID está en la lista de UUIDs autorizados.
 *
 * Esta función compara el UUID recibido con la lista de UUIDs
 * autorizados definida en authorizedUuids[].
 *
 * NOTA: Para sistemas con muchos usuarios, considera implementar:
 * - Almacenamiento en SPIFFS/SD card
 * - Consulta a servidor remoto vía WiFi
 * - Base de datos SQLite en ESP32
 *
 * @param uuid String con el UUID a verificar
 * @return true si está autorizado, false en caso contrario
 */
bool isAuthorized(String uuid) {
    int numUuids = sizeof(authorizedUuids) / sizeof(authorizedUuids[0]);

    Serial.print("[AUTH] Verificando UUID contra ");
    Serial.print(numUuids);
    Serial.println(" UUIDs autorizados...");

    for (int i = 0; i < numUuids; i++) {
        if (uuid.equalsIgnoreCase(authorizedUuids[i])) {
            Serial.print("[AUTH] ✓ UUID autorizado (posición ");
            Serial.print(i);
            Serial.println(")");
            return true;
        }
    }

    Serial.println("[AUTH] ✗ UUID NO autorizado");
    return false;
}

// ============================================================================
// FUNCIONES DE RESPUESTA BLE
// ============================================================================

/**
 * Códigos de respuesta enviados al cliente Android.
 * Estos códigos permiten que la app sepa el resultado de la operación.
 */
#define RESPONSE_OK      "OK"       // Acceso autorizado, pluma activada
#define RESPONSE_DENIED  "DENIED"   // UUID no autorizado
#define RESPONSE_ERROR   "ERROR"    // Error de formato u otro

/**
 * Envía una respuesta al cliente Android mediante NOTIFY.
 *
 * Esta función permite que la app Android sepa el resultado de la operación:
 * - "OK": Autorizado, la pluma se abrió
 * - "DENIED": UUID no autorizado
 * - "ERROR": Formato inválido u otro error
 *
 * @param response String con el código de respuesta
 */
void sendResponse(const char* response) {
    if (pCharacteristic != NULL && deviceConnected) {
        pCharacteristic->setValue(response);
        pCharacteristic->notify();
        Serial.print("[BLE] Respuesta enviada al cliente: ");
        Serial.println(response);
    }
}

// ============================================================================
// FUNCIONES DE CONTROL DE HARDWARE
// ============================================================================

/**
 * Activa el relé para abrir la pluma del estacionamiento.
 *
 * Secuencia:
 * 1. Activa el relé (HIGH)
 * 2. Espera RELAY_ACTIVE_TIME milisegundos
 * 3. Desactiva el relé (LOW)
 *
 * IMPORTANTE: Ajusta RELAY_ACTIVE_TIME según tu hardware:
 * - Demasiado corto: La pluma no se abre completamente
 * - Demasiado largo: Desperdicio de energía y desgaste del motor
 */
void activateGate() {
    Serial.println("\n╔════════════════════════════════════╗");
    Serial.println("║   ACTIVANDO PLUMA                  ║");
    Serial.println("╚════════════════════════════════════╝");

    // Activar relé
    digitalWrite(RELAY_PIN, HIGH);
    Serial.print("[RELAY] Estado: ON - Duración: ");
    Serial.print(RELAY_ACTIVE_TIME);
    Serial.println(" ms");

    // Esperar tiempo configurado
    delay(RELAY_ACTIVE_TIME);

    // Desactivar relé
    digitalWrite(RELAY_PIN, LOW);
    Serial.println("[RELAY] Estado: OFF");

    Serial.println("╔════════════════════════════════════╗");
    Serial.println("║   PLUMA ACTIVADA EXITOSAMENTE      ║");
    Serial.println("╚════════════════════════════════════╝\n");
}

/**
 * Maneja el comando de apertura recibido desde la app.
 *
 * Esta función orquesta todo el proceso de validación y apertura:
 * 1. Valida el formato del UUID
 * 2. Verifica la autorización (o acepta cualquier UUID en TEST_MODE)
 * 3. Activa la pluma si pasa todas las validaciones
 * 4. Envía respuesta al cliente Android
 *
 * @param userUuid UUID del usuario que solicita la apertura
 */
void handleOpenCommand(String userUuid) {
    Serial.println("\n━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");
    Serial.println("  COMANDO DE APERTURA RECIBIDO");
    Serial.println("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");
    Serial.print("UUID Recibido: ");
    Serial.println(userUuid);

    // Mostrar UUID en formato fácil de copiar para agregar a lista de autorizados
    Serial.println("\n╔═══════════════════════════════════════════════╗");
    Serial.println("║  COPIA ESTE UUID PARA AGREGARLO A LA LISTA:   ║");
    Serial.print("║  \"");
    Serial.print(userUuid);
    Serial.println("\"");
    Serial.println("╚═══════════════════════════════════════════════╝");

    // PASO 1: Validar formato del UUID
    Serial.println("\n[VALIDACIÓN 1/2] Verificando formato...");
    if (!isValidUuidFormat(userUuid)) {
        Serial.println("\n❌ RECHAZADO: Formato de UUID inválido");
        Serial.println("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━\n");
        sendResponse(RESPONSE_ERROR);
        return;
    }

    // PASO 2: Validar autorización (o skip en TEST_MODE)
    Serial.println("\n[VALIDACIÓN 2/2] Verificando autorización...");

    #if TEST_MODE
        Serial.println("╔═══════════════════════════════════════════════╗");
        Serial.println("║  ⚠️  MODO TEST ACTIVO                         ║");
        Serial.println("║  Aceptando cualquier UUID con formato válido  ║");
        Serial.println("║  Desactiva TEST_MODE para producción          ║");
        Serial.println("╚═══════════════════════════════════════════════╝");
        Serial.println("[AUTH] ✓ TEST_MODE: UUID aceptado automáticamente");
    #else
        if (!isAuthorized(userUuid)) {
            Serial.println("\n❌ RECHAZADO: UUID no autorizado");
            Serial.println("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━\n");
            sendResponse(RESPONSE_DENIED);
            return;
        }
    #endif

    // PASO 3: Todas las validaciones pasadas, abrir pluma
    Serial.println("\n✓ VALIDACIONES COMPLETADAS");
    Serial.println("✓ Acceso autorizado");

    // Enviar respuesta de éxito al cliente ANTES de activar la pluma
    sendResponse(RESPONSE_OK);

    // Activar la pluma
    activateGate();

    Serial.println("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━\n");
}

// ============================================================================
// CONFIGURACIÓN INICIAL (SETUP)
// ============================================================================

/**
 * Función de configuración inicial del ESP32.
 *
 * Se ejecuta una vez al encender o reiniciar el dispositivo.
 * Configura:
 * - Puerto serial para debugging
 * - Pines GPIO (relé y LED)
 * - Servidor BLE
 * - Servicio y característica BLE
 * - Advertising BLE
 */
void setup() {
    // Inicializar comunicación serial
    Serial.begin(115200);
    delay(1000); // Esperar estabilización

    // Mensaje de bienvenida
    Serial.println("\n\n╔════════════════════════════════════════════╗");
    Serial.println("║                                            ║");
    Serial.println("║   ESP32 PARKING GATE CONTROLLER v1.0      ║");
    Serial.println("║                                            ║");
    Serial.println("╚════════════════════════════════════════════╝\n");

    Serial.println("[INIT] Iniciando sistema...\n");

    // ─────────────────────────────────────────────────────────────────────
    // CONFIGURACIÓN DE HARDWARE
    // ─────────────────────────────────────────────────────────────────────
    Serial.println("[HARDWARE] Configurando pines GPIO...");
    pinMode(RELAY_PIN, OUTPUT);
    pinMode(LED_PIN, OUTPUT);
    digitalWrite(RELAY_PIN, LOW);  // Relé apagado por defecto
    digitalWrite(LED_PIN, LOW);     // LED apagado
    Serial.println("[HARDWARE] ✓ Pines configurados");

    // ─────────────────────────────────────────────────────────────────────
    // CONFIGURACIÓN BLE
    // ─────────────────────────────────────────────────────────────────────
    Serial.println("\n[BLE] Inicializando Bluetooth Low Energy...");
    BLEDevice::init(DEVICE_NAME);
    Serial.print("[BLE] Nombre del dispositivo: ");
    Serial.println(DEVICE_NAME);

    // Crear servidor BLE
    pServer = BLEDevice::createServer();
    pServer->setCallbacks(new MyServerCallbacks());
    Serial.println("[BLE] ✓ Servidor BLE creado");

    // Crear servicio BLE
    BLEService *pService = pServer->createService(SERVICE_UUID);
    Serial.print("[BLE] ✓ Servicio creado con UUID: ");
    Serial.println(SERVICE_UUID);

    // Crear característica BLE
    // Propiedades: READ (lectura), WRITE (escritura) y NOTIFY (enviar respuestas)
    pCharacteristic = pService->createCharacteristic(
        CHARACTERISTIC_UUID,
        BLECharacteristic::PROPERTY_READ |
        BLECharacteristic::PROPERTY_WRITE |
        BLECharacteristic::PROPERTY_NOTIFY
    );

    pCharacteristic->setCallbacks(new MyCharacteristicCallbacks());
    pCharacteristic->addDescriptor(new BLE2902());
    Serial.print("[BLE] ✓ Característica creada con UUID: ");
    Serial.println(CHARACTERISTIC_UUID);
    Serial.println("[BLE] ✓ Propiedades: READ, WRITE, NOTIFY");

    // Iniciar servicio
    pService->start();
    Serial.println("[BLE] ✓ Servicio iniciado");

    // Configurar y iniciar advertising
    BLEAdvertising *pAdvertising = BLEDevice::getAdvertising();
    pAdvertising->addServiceUUID(SERVICE_UUID);
    pAdvertising->setScanResponse(true);
    pAdvertising->setMinPreferred(0x06);  // Intervalo mínimo de conexión
    pAdvertising->setMinPreferred(0x12);  // Intervalo máximo de conexión
    BLEDevice::startAdvertising();
    Serial.println("[BLE] ✓ Advertising iniciado");

    // ─────────────────────────────────────────────────────────────────────
    // SISTEMA LISTO
    // ─────────────────────────────────────────────────────────────────────
    Serial.println("\n╔════════════════════════════════════════════╗");
    Serial.println("║   SISTEMA LISTO                            ║");
    Serial.println("║   Esperando conexiones BLE...              ║");
    Serial.println("╚════════════════════════════════════════════╝\n");

    // Imprimir información de configuración
    Serial.println("─────────── CONFIGURACIÓN ───────────");
    Serial.print("Dispositivo BLE: ");
    Serial.println(DEVICE_NAME);
    Serial.print("Servicio UUID: ");
    Serial.println(SERVICE_UUID);
    Serial.print("Característica UUID: ");
    Serial.println(CHARACTERISTIC_UUID);
    Serial.print("Pin del relé: GPIO ");
    Serial.println(RELAY_PIN);
    Serial.print("Tiempo de activación: ");
    Serial.print(RELAY_ACTIVE_TIME);
    Serial.println(" ms");
    Serial.print("UUIDs autorizados: ");
    Serial.println(sizeof(authorizedUuids) / sizeof(authorizedUuids[0]));
    Serial.println("─────────────────────────────────────\n");

    // Mostrar estado del modo TEST
    #if TEST_MODE
        Serial.println("╔═══════════════════════════════════════════════════════╗");
        Serial.println("║  ⚠️  MODO TEST ACTIVADO                               ║");
        Serial.println("║                                                       ║");
        Serial.println("║  - Cualquier UUID con formato válido será aceptado    ║");
        Serial.println("║  - El UUID recibido se mostrará en Serial             ║");
        Serial.println("║  - Copia el UUID y agrégalo a authorizedUuids[]       ║");
        Serial.println("║  - Para producción: cambia TEST_MODE a 'false'        ║");
        Serial.println("╚═══════════════════════════════════════════════════════╝\n");
    #else
        Serial.println("[INFO] Modo producción - Solo UUIDs autorizados");
        Serial.println("[INFO] Lista de UUIDs autorizados:");
        for (int i = 0; i < sizeof(authorizedUuids) / sizeof(authorizedUuids[0]); i++) {
            Serial.print("  ");
            Serial.print(i + 1);
            Serial.print(". ");
            Serial.println(authorizedUuids[i]);
        }
        Serial.println("");
    #endif

    Serial.println("═══════════════════════════════════════════════════════════");
    Serial.println("  INSTRUCCIONES DE USO:");
    Serial.println("  1. Abre la app Android 'ParkingGate Controller'");
    Serial.println("  2. Asegúrate de tener Bluetooth activado");
    Serial.println("  3. Presiona el botón 'ABRIR'");
    Serial.println("  4. La app se conectará automáticamente al ESP32");
    Serial.println("  5. Si está autorizado, la pluma se abrirá");
    Serial.println("═══════════════════════════════════════════════════════════\n");
}

// ============================================================================
// LOOP PRINCIPAL
// ============================================================================

/**
 * Loop principal del programa.
 *
 * Se ejecuta continuamente mientras el ESP32 esté encendido.
 *
 * NOTA: La mayoría del trabajo se realiza en los callbacks de BLE,
 * por lo que este loop solo imprime información de estado periódicamente.
 */
void loop() {
    // El procesamiento principal se realiza en los callbacks
    // Este loop se usa principalmente para tareas de mantenimiento

    // Imprimir estado de conexión cada 10 segundos
    static unsigned long lastStatusPrint = 0;
    unsigned long currentMillis = millis();

    if (currentMillis - lastStatusPrint >= 10000) {
        lastStatusPrint = currentMillis;

        Serial.print("[STATUS] ");
        Serial.print("Tiempo activo: ");
        Serial.print(currentMillis / 1000);
        Serial.print("s | Estado: ");
        Serial.println(deviceConnected ? "CONECTADO ✓" : "Esperando conexión...");
    }

    // Pequeño delay para no saturar el procesador
    delay(1000);
}

// ============================================================================
// FIN DEL CÓDIGO
// ============================================================================

/*
 * ╔═══════════════════════════════════════════════════════════════════════════╗
 * ║                        GUÍA DE PRUEBA RÁPIDA                               ║
 * ╠═══════════════════════════════════════════════════════════════════════════╣
 * ║                                                                            ║
 * ║  PASO 1: Configurar ESP32                                                  ║
 * ║  ────────────────────────                                                  ║
 * ║  - Asegúrate de tener TEST_MODE en 'true' (línea 44)                      ║
 * ║  - Sube el código al ESP32 con Arduino IDE                                 ║
 * ║  - Abre Serial Monitor a 115200 baudios                                    ║
 * ║  - Deberías ver "SISTEMA LISTO - Esperando conexiones BLE..."             ║
 * ║                                                                            ║
 * ║  PASO 2: Probar la app Android                                             ║
 * ║  ───────────────────────────                                               ║
 * ║  - Instala la app en tu dispositivo Android                                ║
 * ║  - Activa Bluetooth en el teléfono                                         ║
 * ║  - Abre la app y presiona "ABRIR"                                          ║
 * ║  - La app debería:                                                         ║
 * ║    1. Escanear → "Escaneando dispositivo..."                               ║
 * ║    2. Conectar → "Conectando..."                                           ║
 * ║    3. Enviar → "Abriendo pluma..."                                         ║
 * ║    4. Éxito → "¡Listo!"                                                    ║
 * ║                                                                            ║
 * ║  PASO 3: Verificar en Serial Monitor                                       ║
 * ║  ────────────────────────────────                                          ║
 * ║  - Deberías ver "Cliente BLE conectado"                                    ║
 * ║  - Luego "COMANDO DE APERTURA RECIBIDO" con el UUID                        ║
 * ║  - Y finalmente "PLUMA ACTIVADA EXITOSAMENTE"                              ║
 * ║  - El LED del ESP32 parpadeará/encenderá                                   ║
 * ║                                                                            ║
 * ║  PASO 4: Agregar UUID para producción                                      ║
 * ║  ─────────────────────────────────                                         ║
 * ║  - Copia el UUID que aparece en Serial Monitor                             ║
 * ║  - Agrégalo al array authorizedUuids[] en este archivo                     ║
 * ║  - Cambia TEST_MODE a 'false'                                              ║
 * ║  - Sube el código actualizado al ESP32                                     ║
 * ║                                                                            ║
 * ╚═══════════════════════════════════════════════════════════════════════════╝
 *
 * COMPATIBILIDAD CON LA APP ANDROID:
 * ──────────────────────────────────
 * Este firmware es 100% compatible con la app Android "ParkingGate Controller".
 *
 * UUIDs configurados (deben coincidir con Constants.kt en la app):
 * - SERVICE_UUID:        0000abcd-0000-1000-8000-00805f9b34fb  ✓
 * - CHARACTERISTIC_UUID: 0000dcba-0000-1000-8000-00805f9b34fb  ✓
 * - DEVICE_NAME:         ParkingGate                          ✓
 *
 * Flujo de comunicación:
 * 1. App escanea buscando SERVICE_UUID
 * 2. App se conecta al ESP32
 * 3. App descubre servicios y encuentra CHARACTERISTIC_UUID
 * 4. App escribe el UUID del usuario (36 caracteres UTF-8)
 * 5. ESP32 valida y responde con NOTIFY ("OK", "DENIED" o "ERROR")
 * 6. ESP32 activa el relé si está autorizado
 *
 * NOTAS ADICIONALES:
 *
 * 1. SEGURIDAD:
 *    - No expongas los UUIDs autorizados públicamente
 *    - Considera implementar encriptación para datos sensibles
 *    - Implementa rate limiting para prevenir spam
 *
 * 2. PRODUCCIÓN:
 *    - Guarda UUIDs en almacenamiento persistente (SPIFFS/SD)
 *    - Implementa sistema de logs para auditoría
 *    - Agrega watchdog timer para mayor confiabilidad
 *    - ¡IMPORTANTE! Cambia TEST_MODE a 'false'
 *
 * 3. OPTIMIZACIÓN:
 *    - Para muchos usuarios, usa base de datos SQLite
 *    - Considera conexión WiFi para validación remota
 *    - Implementa OTA updates para facilitar mantenimiento
 *
 * 4. HARDWARE:
 *    - Verifica que el relé soporte la corriente del motor
 *    - Usa optoacoplador para aislar ESP32 del circuito de potencia
 *    - Agrega protección contra sobrecorriente
 */
