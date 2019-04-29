import socket
import Network
from Network import *
#创建socket
#绑定地址和端口
a = event_type.timeout

HOST = '127.0.0.1'
PORT = 9090
clientip = ""

configfile = open("config.txt", 'r')
listconfig = configfile.readlines()
for i in range(len(listconfig)):
    con = listconfig[i].split();
    name = con[0]
    if name == 'UDPPort':
        PORT = int(con[2])
    if name == 'FilterError':
        setFilterError(int(con[2]))
    if name == 'FilterLost':
        setFilterLost(int(con[2]))
    if name == 'IP':
        clientip = con[2]

setinputpath("serin.txt")
setoutputpathString("serout.txt")
sock = socket.socket(socket.AF_INET, socket.SOCK_DGRAM)
sock.bind((HOST, PORT))
setsock(sock)
data,addr = sock.recvfrom(1024)
setaddr(addr[0],addr[1])


print("init")
#循环
#关闭链接
protocal5()
sock.close()