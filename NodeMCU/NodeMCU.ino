#include <ESP8266WiFi.h>
#include <SPI.h>
#include <MFRC522.h>

#ifndef STASSID
#define STASSID "HCI_LAB"
#define STAPSK  "abcde12345"
#endif

constexpr uint8_t RST_PIN = 5;
constexpr uint8_t SS_PIN = 4;

MFRC522 rfid(SS_PIN, RST_PIN); // Instance of the class
MFRC522::MIFARE_Key key;

byte nuidPICC[4];

const char* ssid     = STASSID;
const char* password = STAPSK;

//const char* host = "192.168.0.18";
const char* host = "192.168.0.19";
const uint16_t port = 9988;

WiFiClient client;

float dist;

int detect_count = 0;
bool detect_flag = true;

void setup() {
  Serial.begin(115200);

  Serial.println();
  Serial.print("Connecting to ");
  Serial.println(ssid);

  WiFi.mode(WIFI_STA);
  WiFi.begin(ssid, password);

  while (WiFi.status() != WL_CONNECTED) {
    delay(500);
    Serial.print(".");
  }

  Serial.println("");
  Serial.println("WiFi connected");
  Serial.println("IP address: ");
  Serial.println(WiFi.localIP());

  Serial.print("connecting to ");
  Serial.print(host);
  Serial.print(':');
  Serial.println(port);

  if (!client.connect(host, port)) {
    Serial.println("connection failed");
    delay(5000);
    return;
  }

  //RFID 태그
  Serial.print("RFID setup start\r\n");
  SPI.begin(); // SPI 시작
  rfid.PCD_Init(); // RFID 시작

  //초기 키 ID 초기화
  for (byte i = 0; i < 6; i++) {
    key.keyByte[i] = 0xFF;
  }

  Serial.print("RFID setup end\r\n");
}

void loop() {
  dist = analogRead(A0) * 0.0048828125;
  dist = (13 * pow(dist, -1));

  if (dist <= 10 && detect_flag) {
    detect_count++;
    if (detect_count == 3) {
      detect_count = 0;
      detect_flag = false;    //계속 거리를 탐지하여 yolo 요청을 반복하는 것을 방지
      client.print("object_detect");
    }
  }
  else if (dist >= 10 && !detect_flag) {
    detect_count++;
    if (detect_count == 3) {
      detect_count = 0;
      detect_flag = true;    //계속 거리를 탐지하여 yolo 요청을 반복하는 것을 방지
      client.print("object_undetect");
    }
  }

  // 카드가 인식되었다면
  if (rfid.PICC_IsNewCardPresent()) {
    // ID가 읽혀졌다면
    if (rfid.PICC_ReadCardSerial()) {
      rfid_func();
    }
  }

  while (client.available()) {
    char ch = static_cast<char>(client.read());
    if (ch == 'R') { // 차가 나갔을 경우
      Serial.write('X');
      //버퍼 초기화
      for (byte i = 0; i < 4; i++) {
        nuidPICC[i] = 0xFF;
      }
    }
    else {    // 서보모터 제어 값을 받을 경우
      Serial.write(ch); // O 또는 X
    }
  }
}

void printDec(byte *buffer, byte bufferSize) {
  String DecString = "RFID-";
  DecString += "$";
  for (byte i = 0; i < bufferSize; i++) {
    DecString += String(buffer[i], DEC);
  }
  DecString += "$";

  if (client.connected()) {
    client.println(DecString);
  }
}

void rfid_func() {
  // MIFARE 방식인지 확인하고 아니면 리턴
  MFRC522::PICC_Type piccType = rfid.PICC_GetType(rfid.uid.sak);
  if (piccType != MFRC522::PICC_TYPE_MIFARE_MINI &&
      piccType != MFRC522::PICC_TYPE_MIFARE_1K &&
      piccType != MFRC522::PICC_TYPE_MIFARE_4K) {
    return;
  }

  // 만약 바로 전에 인식한 RF 카드와 다르다면..
  if (rfid.uid.uidByte[0] != nuidPICC[0] ||
      rfid.uid.uidByte[1] != nuidPICC[1] ||
      rfid.uid.uidByte[2] != nuidPICC[2] ||
      rfid.uid.uidByte[3] != nuidPICC[3] ) {

    // ID를 저장해둔다.
    for (byte i = 0; i < 4; i++) {
      nuidPICC[i] = rfid.uid.uidByte[i];
    }

    //10진수로 출력
    printDec(rfid.uid.uidByte, rfid.uid.size);
  }

  else { //바로 전에 인식한 것과 동일하다면
    Serial.println("sending OUT INFORMATION to server");

    if (client.connected()) {
      client.println("OUT");
    }

    //버퍼 초기화
    for (byte i = 0; i < 4; i++) {
      nuidPICC[i] = 0xFF;
    }
  }
  // PICC 종료
  rfid.PICC_HaltA();

  // 암호화 종료
  rfid.PCD_StopCrypto1();
}
