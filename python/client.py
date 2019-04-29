# -*- coding: utf-8 -*-
"""
Created on Mon Apr 22 15:01:00 2019

@author: Guo
"""

import socket
import sys
import os
import numpy as np
import math

def get_crc_code(data_str,num,crc):
    for index in range(num):
        ch = ord(data_str[index])
        crc = crc ^(ch << 8)
        for i in range(8):
            if crc & 0x8000:
                crc = (crc<<1) ^ 0x1021
            else:
                crc <<= 1
        crc &= 0xFFFF
    crc_code = np.array([chr(int(crc/256-128)),chr(int(crc%256-128))])
    return crc_code

def get_data_str(file_str, position):
    data_str = file_str[position:position+4]
    return data_str

def get_send_buffer(data_str, crc_code):
    send_buffer = np.array([chr(0),chr(0),chr(0),chr(0),chr(0),chr(0)])
    send_buffer[0] = data_str[0]
    send_buffer[1] = data_str[1]
    send_buffer[2] = data_str[2]
    send_buffer[3] = data_str[3]
    send_buffer[4] = crc_code[0]
    send_buffer[5] = crc_code[1]
    return send_buffer
    
read_file = open("C:\\Users\\Administrator\\Desktop\\hello.c","r")
file_str = read_file.read()
print(file_str[0])
file_size = len(file_str)
#print(file_len)
position = 0
buffer_str = []
server_port = 8888
client_port = 8889

#创建socket
client_sock = socket.socket(socket.AF_INET, socket.SOCK_DGRAM)
target_addr = ('127.0.0.1', server_port)
client_addr = ('127.0.0.1', client_port)
client_sock.bind(client_addr)
print("send file size")

file_size_byte = bytes(file_size)
client_sock.sendto(file_size_byte, target_addr)

while(position<=file_size):
    data_str = get_data_str(file_str, position)
    crc_code = get_crc_code(data_str, 4, 0)
    send_array = get_send_buffer(data_str,crc_code)
    send_str = ''.join(str(i) for i in send_array)
    send_byte = bytes(send_str, encoding='utf-8')
    #发送消息
    client_sock.sendto(send_byte, target_addr)
    print("send to server")
    return_msg_byte, ad = client_sock.recvfrom(1024)
    return_msg_str = str(return_msg_byte)
    print(return_msg_str)
    break