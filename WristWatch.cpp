#include <Wire.h>
#include <MPU6050.h>
#include "BluetoothSerial.h"

MPU6050 mpu;
BluetoothSerial SerialBT;

const int MOTOR_PIN = 18;
const int PIEZO_PIN = 34;

#define TWIST_AXIS   1
#define INVERT_SIGN  1

const float GYRO_SENSITIVITY = 131.0;
const float DEADZONE_DPS       = 40.0;
const float TWIST_THRESHOLD_DPS = 200.0;
const unsigned long GESTURE_COOLDOWN = 600;
unsigned long lastGestureTime = 0;

int16_t baseGx = 0, baseGy = 0, baseGz = 0;
bool calibrated = false;

const int PIEZO_FALL_THRESHOLD = 2500;
const unsigned long FALL_DEBOUNCE = 1000;
unsigned long lastFallTime = 0;
const long ACC_MAG_THRESHOLD = 400000000L;

unsigned long vibeStart = 0;
int vibePhase = 0;

enum VibeState { IDLE, YES_VIBE, NO_VIBE, FALL_VIBE };
VibeState vibeState = IDLE;

void updateVibration() {
  unsigned long now = millis();

  switch (vibeState) {
    case YES_VIBE:
      if (now - vibeStart < 150) {
        digitalWrite(MOTOR_PIN, HIGH);
      } else {
        digitalWrite(MOTOR_PIN, LOW);
        vibeState = IDLE;
      }
      break;

    case NO_VIBE:
      if (vibePhase == 0 && now - vibeStart < 120) {
        digitalWrite(MOTOR_PIN, HIGH);
      } else if (vibePhase == 0 && now - vibeStart < 240) {
        digitalWrite(MOTOR_PIN, LOW);
        vibePhase = 1;
        vibeStart = now;
      } else if (vibePhase == 1 && now - vibeStart < 120) {
        digitalWrite(MOTOR_PIN, HIGH);
      } else {
        digitalWrite(MOTOR_PIN, LOW);
        vibeState = IDLE;
        vibePhase = 0;
      }
      break;

    case FALL_VIBE:
      if (now - vibeStart < 400) {
        digitalWrite(MOTOR_PIN, HIGH);
      } else {
        digitalWrite(MOTOR_PIN, LOW);
        vibeState = IDLE;
      }
      break;

    case IDLE:
    default:
      break;
  }
}

void triggerYes() { vibeState = YES_VIBE; vibeStart = millis(); }
void triggerNo()  { vibeState = NO_VIBE;  vibeStart = millis(); vibePhase = 0; }
void triggerFall(){ vibeState = FALL_VIBE; vibeStart = millis(); }

enum GestureState { REST, MOVING_POS, MOVING_NEG };
GestureState gState = REST;

// ------------- BT HELPERS -------------

void sendGesture(const String &g) {
  String msg = "GESTURE:" + g;
  Serial.println(msg);
  SerialBT.println(msg);   // newline, Android readLine()
}

void sendFallEvent() {
  String msg = "EVENT:FALL";
  Serial.println(msg);
  SerialBT.println(msg);
}

// ------------- CALIBRATION -------------

void calibrateGyro() {
  long sumGx = 0, sumGy = 0, sumGz = 0;

  Serial.println("Calibrating gyro... keep wrist STILL");

  for (int i = 0; i < 200; i++) {
    int16_t ax, ay, az, gx, gy, gz;
    mpu.getMotion6(&ax, &ay, &az, &gx, &gy, &gz);
    sumGx += gx;
    sumGy += gy;
    sumGz += gz;
    delay(5);
  }

  baseGx = sumGx / 200;
  baseGy = sumGy / 200;
  baseGz = sumGz / 200;

  calibrated = true;

  Serial.print("Gyro baseline: ");
  Serial.print(baseGx); Serial.print(", ");
  Serial.print(baseGy); Serial.print(", ");
  Serial.println(baseGz);
  Serial.println("Calibration done!");
}

// ------------- SETUP -------------

void setup() {
  Serial.begin(115200);
  delay(1000);

  Serial.println("GLYTCH - YES/NO Wrist Twist + FALL Detection");

  SerialBT.begin("GLYTCH_Wristband");
  Serial.println("BT Serial started as GLYTCH_Wristband");

  pinMode(MOTOR_PIN, OUTPUT);
  digitalWrite(MOTOR_PIN, LOW);

  pinMode(PIEZO_PIN, INPUT);

  Wire.begin(21, 22);

  Serial.println("Initializing MPU6050...");
  mpu.initialize();
  if (!mpu.testConnection()) {
    Serial.println("❌ MPU6050 connection failed!");
    while (1) { delay(1000); }
  }
  Serial.println("✅ MPU6050 connected.");

  calibrateGyro();
}

// ------------- LOOP -------------

void loop() {
  updateVibration();

  if (!calibrated) return;

  int16_t ax, ay, az, gx, gy, gz;
  mpu.getMotion6(&ax, &ay, &az, &gx, &gy, &gz);

  unsigned long now = millis();

  // FALL
  int piezoVal = analogRead(PIEZO_PIN);

  static unsigned long lastPiezoDebug = 0;
  if (now - lastPiezoDebug > 500) {
    Serial.print("Piezo: ");
    Serial.println(piezoVal);
    lastPiezoDebug = now;
  }

  if (now - lastFallTime > FALL_DEBOUNCE) {
    long accMagSq = (long)ax * ax + (long)ay * ay + (long)az * az;

    if (piezoVal > PIEZO_FALL_THRESHOLD && accMagSq > ACC_MAG_THRESHOLD) {
      Serial.println("⚠ FALL DETECTED");
      sendFallEvent();
      triggerFall();
      lastFallTime = now;
    }
  }

  // YES / NO
  gx -= baseGx;
  gy -= baseGy;
  gz -= baseGz;

  float twistDps;
  if (TWIST_AXIS == 0) twistDps = (float)gx / GYRO_SENSITIVITY;
  else if (TWIST_AXIS == 1) twistDps = (float)gy / GYRO_SENSITIVITY;
  else twistDps = (float)gz / GYRO_SENSITIVITY;

  twistDps *= INVERT_SIGN;

  static unsigned long lastDebug = 0;
  if (now - lastDebug > 500) {
    Serial.print("twist dps: ");
    Serial.println(twistDps);
    lastDebug = now;
  }

  if (now - lastGestureTime < GESTURE_COOLDOWN) {
    return;
  }

  float absTwist = fabs(twistDps);

  if (absTwist < DEADZONE_DPS) {
    gState = REST;
    return;
  }

  if (gState == REST) {
    if (twistDps > TWIST_THRESHOLD_DPS) {
      Serial.println("✅ YES DETECTED");
      sendGesture("YES");
      triggerYes();
      lastGestureTime = now;
      gState = MOVING_POS;
    } else if (twistDps < -TWIST_THRESHOLD_DPS) {
      Serial.println("❌ NO DETECTED");
      sendGesture("NO");
      triggerNo();
      lastGestureTime = now;
      gState = MOVING_NEG;
    }
  }

  delay(5);
}
