#include <WiFi.h>
#include <Wire.h>
#include <Adafruit_BME280.h>
#include <WebSocketsServer.h>
#include <WebServer.h>

// WiFi Setup
const char* ssid = "ESP_Conditions";
const char* password = "12345678";

// WebSocket and HTTP Server
WebSocketsServer webSocket = WebSocketsServer(81);
WebServer httpServer(80);

// BME280 Setup
Adafruit_BME280 bme;

// Relay Pins
const int relayIntakePin = 5;
const int relayExhaustPin = 4;

// Sensor values
float currentTemperature = 0.0;
float currentHumidity = 0.0;
float currentPressure = 1013.0;
float currentAirQuality = 40.0;
String alertMessage = "";

// Timing
unsigned long previousMillis = 0;
const long interval = 3000;

// Mode flag
bool isAutoMode = true;

// Manual threshold values (set via app)
float targetTemp = 22.0;
float targetHumidity = 50.0;
float targetPressure = 1013.0;

void setup() {
  Serial.begin(115200);
  delay(1000);

  if (!bme.begin(0x76)) {
    Serial.println("❌ Could not find BME280 sensor!");
    while (1);
  }

  WiFi.softAP(ssid, password);
  Serial.println("Access Point started");
  Serial.print("IP address: ");
  Serial.println(WiFi.softAPIP());

  pinMode(relayIntakePin, OUTPUT);
  pinMode(relayExhaustPin, OUTPUT);
  digitalWrite(relayIntakePin, HIGH);   // Intake fan always ON
  digitalWrite(relayExhaustPin, LOW);

  webSocket.begin();
  webSocket.onEvent(webSocketEvent);

  // Sensor Data Endpoint
  httpServer.on("/data", HTTP_GET, []() {
    String json = "{";
    json += "\"temperature\":" + String(currentTemperature, 1) + ",";
    json += "\"humidity\":" + String(currentHumidity, 1) + ",";
    json += "\"pressure\":" + String(currentPressure, 1) + ",";
    json += "\"airQuality\":" + String(currentAirQuality, 1) + ",";
    json += "\"alerts\":\"" + alertMessage + "\",";
    json += "\"mode\":\"" + String(isAutoMode ? "auto" : "manual") + "\"";
    json += "}";
    httpServer.send(200, "application/json", json);
  });

  // Control Mode Endpoint
  httpServer.on("/mode", HTTP_GET, []() {
    if (httpServer.hasArg("type")) {
      String mode = httpServer.arg("type");
      if (mode == "auto") {
        isAutoMode = true;
        Serial.println("🟢 AUTO mode");
        httpServer.send(200, "text/plain", "Mode set to AUTO");
      } else if (mode == "manual") {
        isAutoMode = false;
        Serial.println("🟠 MANUAL mode");
        httpServer.send(200, "text/plain", "Mode set to MANUAL");
      } else {
        httpServer.send(400, "text/plain", "Invalid mode");
      }
    } else {
      httpServer.send(400, "text/plain", "Missing mode type");
    }
  });

  // Fan 1 - Always ON
  httpServer.on("/fan1", HTTP_GET, []() {
    httpServer.send(200, "text/plain", "Intake fan always ON");
  });

  // Fan 2 - Exhaust Fan Control (Manual Only)
  httpServer.on("/fan2", HTTP_GET, []() {
    if (httpServer.hasArg("state")) {
      if (isAutoMode) {
        httpServer.send(403, "text/plain", "Auto mode – can't override");
        return;
      }
      String state = httpServer.arg("state");
      digitalWrite(relayExhaustPin, state == "on" ? HIGH : LOW);
      Serial.println("Fan2 (Exhaust): " + state);
      httpServer.send(200, "text/plain", "Fan2 " + state);
    } else {
      httpServer.send(400, "text/plain", "Missing state");
    }
  });

  // Accept Default Conditions from App
  httpServer.on("/set_conditions", HTTP_GET, []() {
    if (httpServer.hasArg("temp")) targetTemp = httpServer.arg("temp").toFloat();
    if (httpServer.hasArg("humidity")) targetHumidity = httpServer.arg("humidity").toFloat();
    if (httpServer.hasArg("pressure")) targetPressure = httpServer.arg("pressure").toFloat();
    Serial.printf("📥 New manual conditions -> Temp: %.1f, Humidity: %.1f, Pressure: %.1f\n", targetTemp, targetHumidity, targetPressure);
    httpServer.send(200, "text/plain", "Manual conditions updated");
  });

  httpServer.begin();
  randomSeed(analogRead(0));
}

void loop() {
  webSocket.loop();
  httpServer.handleClient();

  unsigned long currentMillis = millis();
  if (currentMillis - previousMillis >= interval) {
    previousMillis = currentMillis;
    updateSensorData();
    if (isAutoMode) controlFansAutomatically();
    sendSensorData();
  }
}

void updateSensorData() {
  // Read real sensor data
  currentTemperature = bme.readTemperature();
  currentHumidity = bme.readHumidity();
  currentPressure = bme.readPressure() / 100.0F;

  // Add small random drift for dynamic simulation
  currentTemperature += random(-10, 11) * 0.1;  // ±1.0°C
  currentHumidity += random(-10, 11) * 0.2;     // ±2%
  currentPressure += random(-5, 6) * 0.5;       // ±2.5 hPa

  // Clamp values to realistic bounds
  currentTemperature = constrain(currentTemperature, -10.0, 50.0);
  currentHumidity = constrain(currentHumidity, 0.0, 100.0);
  currentPressure = constrain(currentPressure, 950.0, 1050.0);

  // Simulate air quality smoothly
  currentAirQuality += random(-10, 11) * 1.0;  // ±10 units
  currentAirQuality = constrain(currentAirQuality, 10.0, 200.0);

  alertMessage = "";

  // Thresholds depend on mode
  float tempLimit = isAutoMode ? 35.0 : targetTemp;
  float humLow = isAutoMode ? 25.0 : targetHumidity - 10;
  float humHigh = isAutoMode ? 80.0 : targetHumidity + 10;
  float presLow = isAutoMode ? 980.0 : targetPressure - 20;
  float presHigh = isAutoMode ? 1040.0 : targetPressure + 20;

  if (currentTemperature > tempLimit) alertMessage += "High Temperature; ";
  if (currentHumidity < humLow || currentHumidity > humHigh) alertMessage += "Humidity out of range; ";
  if (currentPressure < presLow || currentPressure > presHigh) alertMessage += "Pressure abnormal; ";
  if (currentAirQuality > 150.0) alertMessage += "Poor Air Quality; ";

  if (alertMessage.endsWith("; ")) {
    alertMessage = alertMessage.substring(0, alertMessage.length() - 2);
  }

  Serial.printf("🌡️ Temp: %.1f°C | 💧 Humidity: %.1f%% | ⬇️ Pressure: %.1f hPa | AQ: %.1f\n",
                currentTemperature, currentHumidity, currentPressure, currentAirQuality);
}

void controlFansAutomatically() {
  if (currentTemperature > 30.0) {
    digitalWrite(relayExhaustPin, HIGH);
    Serial.println("AUTO: Exhaust ON");
  } else {
    digitalWrite(relayExhaustPin, LOW);
    Serial.println("AUTO: Exhaust OFF");
  }
}

void sendSensorData() {
  String json = "{";
  json += "\"temperature\":" + String(currentTemperature, 1) + ",";
  json += "\"humidity\":" + String(currentHumidity, 1) + ",";
  json += "\"pressure\":" + String(currentPressure, 1) + ",";
  json += "\"airQuality\":" + String(currentAirQuality, 1) + ",";
  json += "\"alerts\":\"" + alertMessage + "\",";
  json += "\"mode\":\"" + String(isAutoMode ? "auto" : "manual") + "\"";
  json += "}";
  webSocket.broadcastTXT(json);
  Serial.println("📤 Sent: " + json);
}

void webSocketEvent(uint8_t client_num, WStype_t type, uint8_t * payload, size_t length) {
  if (type == WStype_CONNECTED) {
    Serial.printf("Client %u connected\n", client_num);
  } else if (type == WStype_DISCONNECTED) {
    Serial.printf("Client %u disconnected\n", client_num);
  }
}
