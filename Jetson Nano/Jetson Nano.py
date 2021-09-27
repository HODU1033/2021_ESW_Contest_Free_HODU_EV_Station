import socket

import cvlib as cv

from cvlib.object_detection import draw_bbox

import cv2

from _thread import *

import threading

import time

 

HOST = '192.168.0.19'

PORT = 9988

 

station_request = [False, False, False]

 

lock = threading.Lock()

 

client_socket = socket.socket(socket.AF_INET, socket.SOCK_STREAM)

client_socket.connect((HOST,PORT))

print('Connected to Server!')

 

def threaded(YOLO_thread, client_socket):

	global station_request

 

	print('YOLO on')

	while True:

		if station_request[0] or station_request[1] or station_request[2]:

			print('YOLO start')

			webcam = cv2.VideoCapture(0, cv2.CAP_V4L)

 

			if not webcam.isOpened():

				print("Could not open webcam")

				exit()

 

			webcam.set(cv2.CAP_PROP_FRAME_WIDTH, 640)

			webcam.set(cv2.CAP_PROP_FRAME_HEIGHT, 360)

 

			# loop through frames

			while webcam.isOpened():

        			# read frame from webcam 

				status, frame = webcam.read()

				if not status:

					break

 

				bbox, label, conf = cv.detect_common_objects(frame, model='yolov3-tiny', enable_gpu=True)

				if not len(label) == 0:

					station = [[0 for _ in range(7)] for _ in range(len(label))]

					for i in range(0, len(label)):

						if label[i] == 'cell phone':

							label[i] = 'car'

						station[i][0] = label[i]

 

						for j in range(0, 4):

							station[i][j + 1] = bbox[i][j]

							if j == 0:

								station[i][j + 5] = int((bbox[i][j] + bbox[i][j + 2]) / 2)

							if j == 1:

								station[i][j + 5] = int((bbox[i][j] + bbox[i][j + 2]) / 2)		

 

						if station[i][0] == 'car':

							if station_request[0] and (150 < station[i][5] and station[i][5] < 260):

								client_socket.send('car correct-1'.encode('utf-8'))

								lock.acquire()

								station_request[0] = False

								lock.release()

								time.sleep(1)

							if station_request[1] and (270 < station[i][5] and station[i][5] < 380):

								client_socket.send('car correct-2'.encode('utf-8'))

								lock.acquire()

								station_request[1] = False

								lock.release()

								time.sleep(1)

							if station_request[2] and (390 < station[i][5] and station[i][5] < 500):

								client_socket.send('car correct-3'.encode('utf-8'))

								lock.acquire()

								station_request[2] = False

								lock.release()

								time.sleep(1)

 

					print(station)

      				# draw bounding box over detected objects (검출된 물체 가장자리에 바운딩 박스 그리기)

				if station_request[0]:

					frame = cv2.rectangle(frame, (150, 100), (260, 210), (0, 0, 255), 2)

				if station_request[1]:

					frame = cv2.rectangle(frame, (270, 100), (380, 210), (0, 0, 255), 2)

				if station_request[2]:

					frame = cv2.rectangle(frame, (390, 100), (500, 210), (0, 0, 255), 2)

				show_window = draw_bbox(frame, bbox, label, conf, write_conf=True)

				cv2.imshow("Real-time object detection", show_window)

 

        			# press "Q" to stop and end request

				if cv2.waitKey(1) & 0xFF == ord('q') or not (station_request[0] or station_request[1] or station_request[2]):

					print('YOLO stop')

					break

 

			# release resources

			webcam.release()

 

#thread start

start_new_thread(threaded, (1, client_socket))

 

#TCP start

print('TCP start')

while True:

	data = client_socket.recv(1024)

 

	data_string = data.decode('utf-8')

	print('RECV : ', data_string)

 

	if (data_string.startswith('yolo_request-')):

		station_num = int(data_string.split('yolo_request-')[1])

		lock.acquire()

		station_request[station_num - 1] = True

		lock.release()

	if (data_string.startswith('yolo_request_cancel-')):

		station_num = int(data_string.split('yolo_request_cancel-')[1])

		lock.acquire()

		station_request[station_num - 1] = False

		lock.release()

 

client_socket.close()