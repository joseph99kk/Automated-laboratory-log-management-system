#include <WiFi.h>
#include <WebServer.h>
#include <WebSocketsServer.h>
#include <Wire.h>
#include <Adafruit_Sensor.h>
#include <Adafruit_BME280.h>

// ========== Configuration ==========
const char* ssid = "Nuero Vent";
const char* password = "12345678";

// ========== Sensor Objects ==========
Adafruit_BME280 bme;  // BME280 sensor object

// ========== Server Objects ==========
WebServer server(80);
WebSocketsServer webSocket = WebSocketsServer(81);

// ========== Environment Variables ==========
float targetTemp = 26.0;
float targetHumidity = 65.0;
float targetPressure = 880.1;

// ========== Current Sensor Readings ==========
float currentInsideTemp = 0.0;
float currentInsideHumidity = 0.0;
float currentPressure = 0.0;
int currentAirQuality = 50; // Dummy air quality value (0-100 scale)

// ========== Timing Variables ==========
unsigned long lastSensorUpdate = 0;
const unsigned long sensorInterval = 3000; // 3 seconds

// ========== Helper Functions ==========
void printHeader(const char* title) {
  Serial.println("\n========================================");
  Serial.print("=== "); Serial.print(title); Serial.println(" ===");
  Serial.println("========================================");
}

void printStatus(const char* item, const char* status) {
  Serial.print("[STATUS] "); 
  Serial.print(item);
  Serial.print(": ");
  Serial.println(status);
}

void printValue(const char* name, float value, const char* unit = "") {
  Serial.print("[DATA] ");
  Serial.print(name);
  Serial.print(": ");
  Serial.print(value);
  Serial.println(unit);
}

// ========== Core Functions ==========
void setup() {
  Serial.begin(115200);
  delay(1000); // Wait for serial to initialize

  printHeader("Nuero Vent System Startup");
  printStatus("System", "Initializing...");

  // Initialize I2C
  printStatus("I2C Bus", "Starting...");
  Wire.begin(21, 22); // SDA, SCL
  scanI2CBus();

  // Initialize BME280
  printStatus("BME280 Sensor", "Searching...");
  if (!bme.begin(0x76)) {
    printStatus("BME280", "Not found at 0x76, trying 0x77...");
    if (!bme.begin(0x77)) {
      printStatus("BME280", "ERROR: Sensor not found!");
    } else {
      printStatus("BME280", "Found at 0x77");
    }
  } else {
    printStatus("BME280", "Found at 0x76");
  }

  // Start WiFi AP
  printStatus("WiFi AP", "Starting...");
  WiFi.softAP(ssid, password);
  Serial.print("[NETWORK] AP \"");
  Serial.print(ssid);
  Serial.print("\" started at IP: ");
  Serial.println(WiFi.softAPIP());

  // Start servers
  printStatus("HTTP Server", "Starting on port 80...");
  setupHttpServer();
  server.begin();

  printStatus("WebSocket Server", "Starting on port 81...");
  webSocket.begin();
  webSocket.onEvent(webSocketEvent);

  printStatus("System", "Ready");
  printHeader("Running System");
}

void loop() {
  server.handleClient();
  webSocket.loop();

  // Update sensor data at regular intervals
  if (millis() - lastSensorUpdate >= sensorInterval) {
    lastSensorUpdate = millis();
    readSensors();
  }
}

void readSensors() {
  Serial.println("\n[SENSOR] Reading data...");
  
  float temp = bme.readTemperature();
  float hum = bme.readHumidity();
  float pres = bme.readPressure() / 100.0F;
  
  bool changed = false;

  if (!isnan(temp) && abs(temp - currentInsideTemp) > 0.1) {
    currentInsideTemp = temp;
    printValue("Temperature", currentInsideTemp, "°C");
    changed = true;
  }

  if (!isnan(hum) && abs(hum - currentInsideHumidity) > 0.5) {
    currentInsideHumidity = hum;
    printValue("Humidity", currentInsideHumidity, "%");
    changed = true;
  }

  if (!isnan(pres) && abs(pres - currentPressure) > 0.5) {
    currentPressure = pres;
    printValue("Pressure", currentPressure, "hPa");
    changed = true;
  }

  // Simulate air quality with a dummy value
  int newAirQuality = random(30, 80);
  if (abs(newAirQuality - currentAirQuality) > 5) {
    currentAirQuality = newAirQuality;
    printValue("Air Quality", currentAirQuality, "/100");
    changed = true;
  }

  if (changed) {
    Serial.println("[SENSOR] Values changed - broadcasting update");
    sendSensorData();
  } else {
    Serial.println("[SENSOR] No significant changes detected");
  }
}

void sendSensorData() {
  String json = "{";
  json += "\"temperature\":" + String(currentInsideTemp, 1) + ",";
  json += "\"humidity\":" + String(currentInsideHumidity, 1) + ",";
  json += "\"pressure\":" + String(currentPressure, 1) + ",";
  json += "\"airQuality\":" + String(currentAirQuality) + ",";
  json += "\"targetTemp\":" + String(targetTemp, 1) + ",";
  json += "\"targetHumidity\":" + String(targetHumidity, 1) + ",";
  json += "\"targetPressure\":" + String(targetPressure, 1);
  json += "}";
  
  webSocket.broadcastTXT(json);
  Serial.println("[WEBSOCKET] Broadcasted data to all clients");
  Serial.print("[DATA] ");
  Serial.println(json);
}

// ========== WebSocket Functions ==========
void webSocketEvent(uint8_t num, WStype_t type, uint8_t * payload, size_t length) {
  switch(type) {
    case WStype_DISCONNECTED:
      Serial.printf("[WEBSOCKET] Client #%u disconnected\n", num);
      break;
      
    case WStype_CONNECTED:
      {
        IPAddress ip = webSocket.remoteIP(num);
        Serial.printf("[WEBSOCKET] Client #%u connected from %d.%d.%d.%d\n", 
                     num, ip[0], ip[1], ip[2], ip[3]);
        sendSensorData();
      }
      break;
      
    case WStype_TEXT:
      Serial.printf("[WEBSOCKET] Received message from #%u: %s\n", num, payload);
      handleWebSocketMessage(num, payload, length);
      break;
      
    default:
      break;
  }
}

void handleWebSocketMessage(uint8_t num, uint8_t * payload, size_t length) {
  String message = String((char*)payload).substring(0, length);
  
  if (message.startsWith("SET_TEMP:")) {
    targetTemp = message.substring(9).toFloat();
    Serial.printf("[SETTING] New temperature target: %.1f°C\n", targetTemp);
    sendSensorData();
  }
  else if (message.startsWith("SET_HUMIDITY:")) {
    targetHumidity = message.substring(13).toFloat();
    Serial.printf("[SETTING] New humidity target: %.1f%%\n", targetHumidity);
    sendSensorData();
  }
  else if (message.startsWith("SET_PRESSURE:")) {
    targetPressure = message.substring(13).toFloat();
    Serial.printf("[SETTING] New pressure target: %.1fhPa\n", targetPressure);
    sendSensorData();
  }
}

// ========== HTTP Server Functions ==========
void setupHttpServer() {
  server.on("/data", []() {
    Serial.println("[HTTP] Handling /data request");
    String json = "{";
    json += "\"temperature\":" + String(currentInsideTemp, 1) + ",";
    json += "\"humidity\":" + String(currentInsideHumidity, 1) + ",";
    json += "\"pressure\":" + String(currentPressure, 1) + ",";
    json += "\"airQuality\":" + String(currentAirQuality) + ",";
    json += "\"targetTemp\":" + String(targetTemp, 1) + ",";
    json += "\"targetHumidity\":" + String(targetHumidity, 1) + ",";
    json += "\"targetPressure\":" + String(targetPressure, 1);
    json += "}";
    server.send(200, "application/json", json);
  });

  server.on("/set_conditions", []() {
    Serial.println("[HTTP] Handling /set_conditions request");
    if (server.hasArg("temp") && server.hasArg("humidity") && server.hasArg("pressure")) {
      targetTemp = server.arg("temp").toFloat();
      targetHumidity = server.arg("humidity").toFloat();
      targetPressure = server.arg("pressure").toFloat();
      
      Serial.printf("[SETTING] New targets via HTTP - Temp: %.1f°C, Hum: %.1f%%, Pres: %.1fhPa\n", 
                   targetTemp, targetHumidity, targetPressure);
      
      server.send(200, "application/json", "{\"status\":\"success\",\"message\":\"Conditions updated\"}");
      sendSensorData();
    } else {
      Serial.println("[HTTP] Missing parameters in /set_conditions request");
      server.send(400, "application/json", "{\"status\":\"error\",\"message\":\"Missing parameters\"}");
    }
  });
}

// ========== I2C Utility ==========
void scanI2CBus() {
  printHeader("I2C Bus Scan");
  byte count = 0;
  
  for (byte address = 1; address < 127; ++address) {
    Wire.beginTransmission(address);
    if (Wire.endTransmission() == 0) {
      Serial.print("  Found device at 0x");
      Serial.println(address, HEX);
      count++;
      delay(1);
    }
  }
  
  if (count == 0) {
    Serial.println("  No I2C devices found!");
  } else {
    Serial.printf("  Found %d device(s)\n", count);
  }
}