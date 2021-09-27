import socket # TCP 소켓
import pymysql # MySQL 접근
import random # 난수 생성

# 쓰레드
from _thread import *
import threading

# 결제 시간 체크
import time
import datetime

# Firebase 사용
import firebase_admin
from firebase_admin import credentials
from firebase_admin import db

# firebase
cred = credentials.Certificate(
    "ev-station-test-firebase-adminsdk-vycud-2941e563b0.json")
firebase_admin.initialize_app(cred, {
    'databaseURL': 'https://ev-station-test-default-rtdb.firebaseio.com/'
})
dir = [db.reference(), db.reference(), db.reference()]

# 각 NodeMCU 및 jetson nano IP 주소로 스테이션 구분
station1 = "192.168.0.22"
station2 = "192.168.0.21"
station3 = "192.168.0.23"
jetson_nano = "192.168.0.20"

HOST = '192.168.0.19'
PORT = 9988

server_socket = socket.socket(socket.AF_INET, socket.SOCK_STREAM)
server_socket.setsockopt(socket.SOL_SOCKET, socket.SO_REUSEADDR, 1)
server_socket.bind((HOST, PORT))
server_socket.listen()
print('server start')

# yolo 요청 스테이션 번호 및 결과 / 배터리 잔량
yolo_request_station = [0, 0, 0] # 0-대기 / 1-요청 / 2-요청취소
yolo_result = [False, False, False]
check_car = [False, False, False]
battery = [-1, -1, -1] # -1 대기

# 동기화 Mutex 선언
lock = threading.Lock()

# 스테이션에 들어온 차량을 가정한 프로세스
def scarecrow_car(station_num):
    global battery
    global check_car
    
    print('scarecrow_car start')
    while True:
        if check_car[station_num]:
            lock.acquire()
            battery[station_num] = random.randrange(10, 60)
            lock.release()
            while check_car[station_num] and battery[station_num] <= 100:
                time.sleep(3)
                lock.acquire()
                battery[station_num] += 1
                lock.release()
            lock.acquire()
            battery[station_num] = -1
            lock.release()

# 스테이션별로 상황을 판단하게 됩니다.
def station_threaded(client_socket, addr):
    # 클라이언트가 접속을 끊을 때 까지 반복합니다.
    if addr[0] == station1:
        station_numb = '1'
        station_num = 0
    elif addr[0] == station2:
        station_numb = '2'
        station_num = 1
    elif addr[0] == station3:
        station_numb = '3'
        station_num = 2
    else:
        station_numb = 'Station_ERROR'
    # addr[0] - 클라이언트의 ip 주소 / addr[1] - 클라이언트의 포트
    print('Connected by : [IP - ', addr[0], ', PORT-', addr[1], '] / Station_Num - ' + station_numb)
    client_socket.settimeout(1)

    # 각 파이어베이스 내 스테이션 db에 접근
    global db
    global dir
    dir[station_num] = db.reference('Station' + station_numb)

    # RFID 플래그 지역 변수 값 초기화
    RFID_tag_match = False

    # 글로벌 변수 선언 및 플래그 초기화
    global yolo_request_station
    global yolo_result
    global battery
    global check_car
    battery_temp = -1
    over_time_flag = False
    over_time = time.localtime()
    count = True

    # 허수아비 차량 생성
    start_new_thread(scarecrow_car, (station_num, ))

    while True:
        #배터리 체크
        if not battery[station_num] == battery_temp:
            battery_temp = battery[station_num]
            print('station' + station_numb + ' battery : ' + str(battery_temp))
            dir[station_num] = db.reference('Station' + station_numb)
            dir[station_num].update({'BATTERY': str(battery_temp)})
        if battery_temp == 100 and not over_time_flag:
            print('over parikng')
            dir[station_num] = db.reference('Station' + station_numb)
            dir[station_num].update({'OVER_CHARGING': 'O'})
            time.sleep(1)
            dir[station_num].update({'OVER_CHARGING': 'X'})
            lock.acquire()
            check_car[station_num] = False
            lock.release()
            over_time_flag = True
            over_time = datetime.datetime.strptime(
                time.strftime('%Y-%m-%d %I:%M:%S %p', time.localtime()), '%Y-%m-%d %I:%M:%S %p')

        try:
            data_byte = client_socket.recv(1024)
            if not data_byte:
                print('Disconnected by ' + addr[0], ':', addr[1])
                break
            data_string = data_byte.decode('utf-8')
            print('Received from [Station - ' + station_numb, '] :', data_string)
        except ConnectionResetError as e:
            print('Disconnected by ' + addr[0], ':', addr[1])
            break
        except socket.timeout:
            data_string = '.'

        # RFID
        if data_string.startswith('RFID-'):
            RFID_TAG_ID = data_string.split('$')[1] # 태그 값 출력
            print(RFID_TAG_ID)

            # jetson_nano mysql 접근
            mysql_db = pymysql.connect(host="192.168.0.20", user="HODU_EV", password="hodu", database="EV_Information", port=3306)
            cursor = mysql_db.cursor()

            print("EV_CAR Information DB CONNECT for SELECT")

            # jetson_nano_MySql
            cursor.execute("SELECT CAR_NAME FROM EV_Information WHERE TAG_ID in (%s)", (RFID_TAG_ID))

            value = cursor.fetchone()  # 차량 이름값을 가져온다
            if value != None:  # 차량 이름이 조회된다면 (값이 있다면)
                print('EV_CHECK_SUCCESS!')
                RFID_tag_match = True
                for row in value:  # 차량 이름 출력
                    print("Car NAME - ", row)
            else:
                print('EV_CHECK_FAIL')
                RFID_tag_match = False
                print('NOT EV CAR or NOT registered EV CAR')
                # 각 MCU 보드에 'A' 전송 (거리센서, 태그 값 활성화)
                client_socket.send('R'.encode('utf-8'))
                print('Send Station '+station_numb+" [R]")

            mysql_db.commit()
            mysql_db.close()

        # 거리판단
        if (data_string == 'object_detect'):
            lock.acquire()
            yolo_request_station[station_num] = 1
            yolo_result[station_num] = False
            lock.release()
        if data_string == 'object_undetect' or data_string == "OUT":
            if RFID_tag_match and yolo_result[station_num]:
                print('EV_Station - ' + station_numb + ' FINISH USING')

                # 차량 출차 시간
                now_time_string = time.strftime('%Y-%m-%d %I:%M:%S %p', time.localtime())
                dir[station_num].update({'OUT_TIME': now_time_string})

                # 결제 금액 계산
                dir[station_num] = db.reference('Station'+station_numb+'/IN_TIME')
                in_time_string = dir[station_num].get()
                dir[station_num] = db.reference('Station'+station_numb+'/OUT_TIME')
                out_time_string = dir[station_num].get()

                dir[station_num] = db.reference('Station' + station_numb)

                in_time = datetime.datetime.strptime(
                    in_time_string, '%Y-%m-%d %I:%M:%S %p')
                out_time = datetime.datetime.strptime(
                    out_time_string, '%Y-%m-%d %I:%M:%S %p')

                # 완충 후 무단 주차
                if over_time_flag:
                    time_interval = over_time - in_time
                    payment = time_interval.seconds * 300
                    time_interval = out_time - over_time
                    payment += time_interval.seconds * 1000
                else:
                    time_interval = out_time - in_time
                    payment = time_interval.seconds * 300
                payment_string = str(payment)
                dir[station_num].update({'PAYMENT': payment_string})  # 초당 300원

                # 해당 스테이션 Firebase 값 변경
                dir[station_num].update({'USING': 'X'})  # 'Station'+station_numb+'/'+
                dir[station_num].update({'CAR_NAME': 'X'})
                dir[station_num].update({'OVER_CHARGING': 'X'})

                # mysql 접근
                mysql_db = pymysql.connect(host="192.168.0.20", user="HODU_EV",
                                           password="hodu", database="EV_Information", port=3306)  # jetson_nano
                cursor = mysql_db.cursor()

                print("EV_CAR Information DB CONNECT for UPDATE")

                # jetson_nano_MySql
                cursor.execute("SELECT BALANCE FROM EV_Information WHERE TAG_ID in (%s)", (RFID_TAG_ID))

                value = cursor.fetchone()  # 잔액을 가져온다

                if value != None:  # 잔액이 조회된다면 (값이 있다면)
                    print('BALANCE_CHECK_SUCCESS!')

                    for row in value:  # 잔액 변수에 담기
                        balance = row
                    balance = balance - payment

                    cursor.execute(
                        "UPDATE EV_Information SET BALANCE=(%s) WHERE TAG_ID in (%s)" % (balance, RFID_TAG_ID))
                    print('PAY SUCCESS!')
                else:
                    print('BALANCE CHECK FAIL!')

                mysql_db.commit()
                mysql_db.close()

                dir[station_num].update({'IN_TIME': '-'})
                dir[station_num].update({'OUT_TIME': '-'})
                dir[station_num].update({'BATTERY': '-1'})

            # 각 NCU 보드에 'X' 전송 (서보모터 제어)
            client_socket.send('R'.encode('utf-8'))
            print('Send Station '+station_numb+" [X]")

            # 차량이 나갔으므로 RFID_match값 및 yolo 요청 및 count 초기화
            RFID_tag_match = False
            over_time_flag = False
            battery_temp = -1
            count = True
            lock.acquire()
            check_car[station_num] = False
            yolo_request_station[station_num] = 2
            yolo_result[station_num] = False
            lock.release()

        # RFID 값 + 거리 결과 값 + 영상처리 결과 값을 합쳐서 파이어베이스 값을 변경!
        if (yolo_result[station_num] and RFID_tag_match and count):
            print('EV_Station - ' + station_numb + ' START USING')

            # 각 NCU 보드에 'O' 전송 (서보모터 제어)
            client_socket.send('O'.encode('utf-8'))
            print('Send Station '+station_numb+" [O]")

            # 허수아비 차량 배터리 체크 시작
            lock.acquire()
            check_car[station_num] = True
            lock.release()

            # 차량 입차 시간
            now = time.localtime()
            now_time_string = time.strftime('%Y-%m-%d %I:%M:%S %p', now)

            # 파이어베이스 값 수정 (충전차량 들어옴)
            dir[station_num] = db.reference('Station'+station_numb)
            dir[station_num].update({'USING': 'O'})
            dir[station_num].update({'CAR_NAME': row})
            dir[station_num].update({'IN_TIME': now_time_string})
            dir[station_num].update({'PAYMENT': '0'})

            count = False
    client_socket.close()

# 젯슨 나노 쓰레드
def jetson_threaded(client_socket, addr):
    print('Connected by : [IP - ', addr[0],
          ', PORT-', addr[1], '] / Jetson Nano')
    client_socket.settimeout(1)

    global yolo_request_station
    global yolo_result

    while True:
        try:
            # 각 스테이션의 YOLO 요청 경우에 작동
            for i in range(0, 3):
                if yolo_request_station[i] == 1:
                    print('yolo request / Request Station - ' + str(i + 1))
                    client_socket.send(('yolo_request-' + str(i + 1)).encode('utf-8'))
                    lock.acquire()
                    yolo_request_station[i] = 0
                    yolo_result[i] = False
                    lock.release()
                elif yolo_request_station[i] == 2:
                    print('yolo request cancel / Request Station - ' + str(i + 1))
                    client_socket.send(('yolo_request_cancel-' + str(i + 1)).encode('utf-8'))
                    lock.acquire()
                    yolo_request_station[i] = 0
                    yolo_result[i] = False
                    lock.release()

            data_byte = client_socket.recv(1024)
            if not data_byte:
                print('Disconnected by ' + addr[0], ':', addr[1])
                break
            data_string = data_byte.decode('utf-8')
            print('Received from [Jetson Nano] :', data_string)

            # 해당 구역에 자동차가 검출되었다면
            if data_string.startswith('car correct-'):
                lock.acquire()
                yolo_result[int(data_string.split('car correct-')[1]) - 1] = True
                lock.release()
        except ConnectionResetError as e:
            print('Disconnected by ' + addr[0], ':', addr[1])
            break
        except socket.timeout:
            data_string = '.'
    client_socket.close()

# 클라이언트가 접속하면 accept 함수에서 새로운 소켓을 리턴합니다.
# 새로운 쓰레드에서 해당 소켓을 사용하여 통신을 하게 됩니다.
while True:
    print('wait')
    # accept()의 반환값은 (데이터를 주고 받을 수 있는) 소켓 객체, 바인드 된 주소
    client_socket, addr = server_socket.accept()
    if addr[0] == jetson_nano:
        start_new_thread(jetson_threaded, (client_socket, addr))
    else:
        start_new_thread(station_threaded, (client_socket, addr))
