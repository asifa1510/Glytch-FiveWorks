
#include "esp_camera.h"
#include <WiFi.h>
#include <WebServer.h>

#define PWDN_GPIO_NUM     32
#define RESET_GPIO_NUM    -1
#define XCLK_GPIO_NUM      0
#define SIOD_GPIO_NUM     26
#define SIOC_GPIO_NUM     27

#define Y9_GPIO_NUM       35
#define Y8_GPIO_NUM       34
#define Y7_GPIO_NUM       39
#define Y6_GPIO_NUM       36
#define Y5_GPIO_NUM       21
#define Y4_GPIO_NUM       19
#define Y3_GPIO_NUM       18
#define Y2_GPIO_NUM        5
#define VSYNC_GPIO_NUM    25
#define HREF_GPIO_NUM     23
#define PCLK_GPIO_NUM     22

// ==== ESP32 HOTSPOT CONFIG ====
const char* AP_SSID = "GLYTCH_CAM";   // Wi-Fi name
const char* AP_PASS = "12345678";     // password (>= 8 chars)

WebServer server(80);

void startCamera() {
  camera_config_t config;
  config.ledc_channel = LEDC_CHANNEL_0;
  config.ledc_timer   = LEDC_TIMER_0;
  config.pin_d0       = Y2_GPIO_NUM;
  config.pin_d1       = Y3_GPIO_NUM;
  config.pin_d2       = Y4_GPIO_NUM;
  config.pin_d3       = Y5_GPIO_NUM;
  config.pin_d4       = Y6_GPIO_NUM;
  config.pin_d5       = Y7_GPIO_NUM;
  config.pin_d6       = Y8_GPIO_NUM;
  config.pin_d7       = Y9_GPIO_NUM;
  config.pin_xclk     = XCLK_GPIO_NUM;
  config.pin_pclk     = PCLK_GPIO_NUM;
  config.pin_vsync    = VSYNC_GPIO_NUM;
  config.pin_href     = HREF_GPIO_NUM;
  config.pin_sccb_sda = SIOD_GPIO_NUM;
  config.pin_sccb_scl = SIOC_GPIO_NUM;
  config.pin_pwdn     = PWDN_GPIO_NUM;
  config.pin_reset    = RESET_GPIO_NUM;
  config.xclk_freq_hz = 20000000;
  config.pixel_format = PIXFORMAT_JPEG;
  config.frame_size   = FRAMESIZE_QVGA;
  config.jpeg_quality = 12;
  config.fb_count     = 2;

  esp_err_t err = esp_camera_init(&config);
  if (err != ESP_OK) {
    Serial.printf("Camera init failed 0x%x\n", err);
    while (true) { delay(1000); }
  }
}

// just a test text
void handleRoot() {
  server.send(200, "text/plain",
              "GLYTCH ESP32-CAM AP Ready. Call /scan from Android.");
}

// /scan -> "name|dose"
void handleScan() {
  String medicineName = "Paracetamol 500 mg";
  String medicineDose = "Take 1 tablet every 6 hours. Max 4 per day.";
  String payload = medicineName + "|" + medicineDose;

  server.sendHeader("Access-Control-Allow-Origin", "*");
  server.send(200, "text/plain", payload);
}

void setup() {
  Serial.begin(115200);
  Serial.setDebugOutput(true);
  Serial.println();
  Serial.println("GLYTCH ESP32-CAM AP + /scan");

  startCamera();

  // ---- start ACCESS POINT (no campus Wi-Fi!) ----
  WiFi.mode(WIFI_AP);
  if (!WiFi.softAP(AP_SSID, AP_PASS)) {
    Serial.println("Failed to start AP");
    while (true) { delay(1000); }
  }

  IPAddress ip = WiFi.softAPIP();       // normally 192.168.4.1
  Serial.print("AP started. SSID: ");
  Serial.println(AP_SSID);
  Serial.print("AP IP address: ");
  Serial.println(ip);

  server.on("/", handleRoot);
  server.on("/scan", handleScan);
  server.begin();
  Serial.println("HTTP server started on port 80");
  Serial.println("Test: http://192.168.4.1/scan");
}

void loop() {
  server.handleClient();
}
