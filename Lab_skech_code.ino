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

// Relay Pins (3 fans)
const int relayIntakePin = 5;       // Slow intake fan + thermal pad
const int relayExhaustPin1 = 4;     // Exhaust fan 1 (fast)
const int relayExhaustPin2 = 15;    // Exhaust fan 2 (fast)

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

// Manual fan override states (only valid in manual mode)
bool manualIntakeState = false;
bool manualExhaust1State = false;
bool manualExhaust2State = false;

void setup() {
  Serial.begin(115200);
  delay(1000);

  if (!bme.begin(0x76)) {
    Serial.println("Could not find BME280 sensor!");
    while (1);
  }

  WiFi.softAP(ssid, password);
  Serial.println("Access Point started");
  Serial.print("IP address: ");
  Serial.println(WiFi.softAPIP());

  pinMode(relayIntakePin, OUTPUT);
  pinMode(relayExhaustPin1, OUTPUT);
  pinMode(relayExhaustPin2, OUTPUT);

  // Defaults
  digitalWrite(relayIntakePin, HIGH);   // Intake fan + heater ON by default
  digitalWrite(relayExhaustPin1, LOW);
  digitalWrite(relayExhaustPin2, LOW);

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

  // Mode Control Endpoint
  httpServer.on("/mode", HTTP_GET, []() {
    if (httpServer.hasArg("type")) {
      String mode = httpServer.arg("type");
      if (mode == "auto") {
        isAutoMode = true;
        Serial.println("AUTO mode");
        httpServer.send(200, "text/plain", "Mode set to AUTO");
      } else if (mode == "manual") {
        isAutoMode = false;
        Serial.println("MANUAL mode");
        httpServer.send(200, "text/plain", "Mode set to MANUAL");
      } else {
        httpServer.send(400, "text/plain", "Invalid mode");
      }
    } else {
      httpServer.send(400, "text/plain", "Missing mode type");
    }
  });

  // Fan Control Endpoints (manual override)
  httpServer.on("/fan_intake", HTTP_GET, []() {
    if (!isAutoMode) {
      if (httpServer.hasArg("state")) {
        manualIntakeState = (httpServer.arg("state") == "on");
        digitalWrite(relayIntakePin, manualIntakeState ? HIGH : LOW);
        Serial.println(String("Manual Intake Fan: ") + (manualIntakeState ? "ON" : "OFF"));
        httpServer.send(200, "text/plain", String("Intake fan turned ") + (manualIntakeState ? "ON" : "OFF"));
      } else {
        httpServer.send(400, "text/plain", "Missing state");
      }
    } else {
      httpServer.send(403, "text/plain", "Auto mode – can't override fan");
    }
  });

  httpServer.on("/fan_exhaust1", HTTP_GET, []() {
    if (!isAutoMode) {
      if (httpServer.hasArg("state")) {
        manualExhaust1State = (httpServer.arg("state") == "on");
        digitalWrite(relayExhaustPin1, manualExhaust1State ? HIGH : LOW);
        Serial.println(String("Manual Exhaust Fan 1: ") + (manualExhaust1State ? "ON" : "OFF"));
        httpServer.send(200, "text/plain", String("Exhaust fan 1 turned ") + (manualExhaust1State ? "ON" : "OFF"));
      } else {
        httpServer.send(400, "text/plain", "Missing state");
      }
    } else {
      httpServer.send(403, "text/plain", "Auto mode – can't override fan");
    }
  });

  httpServer.on("/fan_exhaust2", HTTP_GET, []() {
    if (!isAutoMode) {
      if (httpServer.hasArg("state")) {
        manualExhaust2State = (httpServer.arg("state") == "on");
        digitalWrite(relayExhaustPin2, manualExhaust2State ? HIGH : LOW);
        Serial.println(String("Manual Exhaust Fan 2: ") + (manualExhaust2State ? "ON" : "OFF"));
        httpServer.send(200, "text/plain", String("Exhaust fan 2 turned ") + (manualExhaust2State ? "ON" : "OFF"));
      } else {
        httpServer.send(400, "text/plain", "Missing state");
      }
    } else {
      httpServer.send(403, "text/plain", "Auto mode – can't override fan");
    }
  });

  // Accept Default Conditions from App
  httpServer.on("/set_conditions", HTTP_GET, []() {
    if (httpServer.hasArg("temp")) targetTemp = httpServer.arg("temp").toFloat();
    if (httpServer.hasArg("humidity")) targetHumidity = httpServer.arg("humidity").toFloat();
    if (httpServer.hasArg("pressure")) targetPressure = httpServer.arg("pressure").toFloat();
    Serial.printf("New manual conditions -> Temp: %.1f, Humidity: %.1f, Pressure: %.1f\n", targetTemp, targetHumidity, targetPressure);
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
  // Read real sensor data from BME280
  currentTemperature = bme.readTemperature();
  currentHumidity = bme.readHumidity();
  currentPressure = bme.readPressure() / 100.0F;

  // Add small random drift for simulation effect
  currentTemperature += random(-10, 11) * 0.1;  // ±1.0°C
  currentHumidity += random(-10, 11) * 0.2;     // ±2%
  currentPressure += random(-5, 6) * 0.5;       // ±2.5 hPa

  // Clamp values to realistic ranges
  currentTemperature = constrain(currentTemperature, -10.0, 50.0);
  currentHumidity = constrain(currentHumidity, 0.0, 100.0);
  currentPressure = constrain(currentPressure, 950.0, 1050.0);

  // Simulate air quality smoothly
  currentAirQuality += random(-10, 11) * 1.0;  // ±10 units
  currentAirQuality = constrain(currentAirQuality, 10.0, 200.0);

  alertMessage = "";

  // Define thresholds depending on mode
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

  Serial.printf("Temp: %.1f°C | Humidity: %.1f%% | Pressure: %.1f hPa | AQ: %.1f\n",
                currentTemperature, currentHumidity, currentPressure, currentAirQuality);
}

void controlFansAutomatically() {
  // Intake fan + heater ON always in auto mode
  digitalWrite(relayIntakePin, HIGH);

  if (currentTemperature > targetTemp) {
    // Turn ON both exhaust fans to create negative pressure
    digitalWrite(relayExhaustPin1, HIGH);
    digitalWrite(relayExhaustPin2, HIGH);
    Serial.println("AUTO: Exhaust Fans ON (negative pressure created)");
  } else {
    // Turn OFF exhaust fans
    digitalWrite(relayExhaustPin1, LOW);
    digitalWrite(relayExhaustPin2, LOW);
    Serial.println("AUTO: Exhaust Fans OFF");
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
  Serial.println("Sent: " + json);
}

void webSocketEvent(uint8_t client_num, WStype_t type, uint8_t * payload, size_t length) {
  if (type == WStype_CONNECTED) {
    Serial.printf("Client %u connected\n", client_num);
  } else if (type == WStype_DISCONNECTED) {
    Serial.printf("Client %u disconnected\n", client_num);
  }
}
