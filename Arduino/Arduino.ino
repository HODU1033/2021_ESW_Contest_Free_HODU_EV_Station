#include <Servo.h>
 
Servo myservo; 

int pos;

void setup(){
    Serial.begin(115200);

    myservo.attach(10);
    pos = 70;
    myservo.write(pos);
}
 
void loop(){
    if(Serial.available()>0){
        char data = Serial.read();

        if(data == 'O') {
          for( ; pos > 2 ; pos--){
            myservo.write(pos);
            delay(5);
          }
        }
        else if (data =='X') {
          for( ; pos <=70 ; pos++){
            myservo.write(pos);
            delay(5);
          }
        }
      }
         
    
    
}
