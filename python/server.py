# -*- coding: utf-8 -*-
"""
Created on Mon Apr 22 09:45:25 2019

@author: Guo
"""


import socket
import sys
import os
import numpy as np
import math
expected_frame = 0


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

def find_error(recv_str):
    data_str = get_data_str(recv_str,0)
    correct_crc = get_crc_code(data_str, 4, 0)
    if (correct_crc[0]==recv_str[4]) and (correct_crc[1]==recv_str[5]):
        return 0
    else:
        return 1
    

##创建 socket 对象
server_sock = socket.socket(socket.AF_INET, socket.SOCK_DGRAM)
server_sock.bind(("127.0.0.1", 8888))

file_size_byte, ad = server_sock.recvfrom(1024)
file_size = 0
file_size = int(file_size_byte)
print("recv file size:", file_size)

#setsock(server_sock)
while(1):
    data, ad = server_sock.recvfrom(1024)
    recv_str = str(data)
    #find_error()
    print(recv_str)
    return_msg = bytes("1", encoding='gbk')
    addr = ('127.0.0.1', 8889)
    server_sock.sendto(return_msg, addr)
    print("server return msg")
    break











