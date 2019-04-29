import socket
from Network import *
#创建socket
inputpath = "clientin.txt"
outputpath = "clientout.txt"
serverip = "a"

configfile = open("config.txt", 'r')
listconfig = configfile.readlines()
for i in range(len(listconfig)):
    print(listconfig[i])
    con = listconfig[i].split();
    name = con[0]
    if name == 'UDPPort':
        PORT = int(con[2])
    if name == 'FilterError':
        setFilterError(int(con[2]))
    if name == 'FilterLost':
        setFilterLost(int(con[2]))
    if name == 'IP':
        serverip = con[2]

sock  = socket.socket(socket.AF_INET, socket.SOCK_DGRAM)
#创建发送消息和发送目标
msg = b'Hello world'
addr = (serverip, PORT)
sock.sendto(msg, addr)
setinputpath("clientin.txt")
setoutputpathString("clientout.txt")
setsock(sock)
setaddr(addr[0],addr[1])


print("init")
protocal5()
sock.close()