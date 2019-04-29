from enum import Enum
import socket
import select

sock = socket.socket(socket.AF_INET, socket.SOCK_DGRAM)
addr = ('127.0.0.1', 9090)

def setsock(so):
    global sock
    sock = so

def setaddr(ip,port):
    global addr
    addr = (ip, port)



class event_type(Enum):
    frame_arrival = 1
    cksum_err = 2
    timeout = 3
    network_layer_ready = 4

networkMessage = True
networkEnable = True

frameError = False
frameLost = False
timerEnd = False

MAX_SEQ = 7
timer_max = 500000
timer = [0]*(MAX_SEQ+1)

MAX_PKT = 3
class packet:
    def __init__(self):
        self.data = []

class frame_kind(Enum):
    data = 1
    ack = 2
    nak = 3


class frame:
    def __init__(self):
        self.frame_kind = frame_kind.data
        self.seq = 0
        self.ack = 0
        self.info = packet()
frame_max_size = MAX_PKT+6
def frame_to_charArray(f):
    ch = "%d %d %d " %(1,f.seq,f.ack)
    ch += ''.join(f.info.data)
    return ch

def charArray_to_frame(ch):
#0 1 1 aaa
    f = frame()
    ls = ch.split(" ")
    f.seq = int(ls[1])
    f.ack = int(ls[2])
    for i in range(len(ch)-6):
        f.info.data.append(ch[i+6])
    return f

def inc(k):
    if k < MAX_SEQ:
        k = k+1
    else:
        k = 0
    return k

FilterError = 10
FilterLost = 10
error = 0
lost = 5
def setFilterError(input):
    global FilterError
    FilterError = input
def setFilterLost(input):
    global FilterLost
    FilterLost = input

def filter():
    global error
    error += 1
    global lost
    lost += 1
    global frameLost
    global frameError

    if error % FilterError == 0:
        frameError = True
        error = 0
    if lost % FilterLost == 0:
        frameLost = False
        lost = 0

receiveOk = True

def wait_for_event():
    global timerEnd
    while(True):
        inputs = [sock]
        outputs = []
        readable, writable, exceptional = select.select(inputs, outputs, inputs, 0)
        if readable:
            return checkRecv()
        if networkMessage and networkEnable:
            return event_type.network_layer_ready
        if timerEnd:
            timerEnd = False
            return event_type.timeout
        add_timer()


def doCRC(addr,num,crc):
    for index in range(num):
        ch = ord(addr[index])
        crc = crc ^(ch << 8)
        for i in range(8):
            if crc & 0x8000:
                crc = (crc<<1) ^ 0x1021
            else:
                crc <<= 1
        crc &= 0xFFFF
    return crc

def doCRCCheck(buf):
    crcResault = doCRC(buf,frame_max_size+2,0)
    if crcResault != 0:
        return False
    return True

latestMessage = []
def checkRecv():
    global latestMessage
    data, a = sock.recvfrom(frame_max_size+10)
    latestMessage = data.decode()

    print("recv msg:",end='')
    if doCRCCheck(latestMessage):
        print("CRC OK")
        return event_type.frame_arrival
    else:
        print("CRC remainder is not 0")
        return event_type.cksum_err

def from_physical_layer():
    data = ""
    for i in range(frame_max_size):
        data+=(latestMessage[i])
    p = charArray_to_frame(data)
    return p

def to_physical_layer(p):
    global frameLost
    global frameError
    global sock
    global addr

    buf = frame_to_charArray(p)
    filter()
    print("after filter:",end='')
    if frameLost:
        print("Lost")
        frameLost = False
        return
    if len(buf) != frame_max_size:
        for i in range(frame_max_size - len(buf)):
            buf += '\0'
    crcResault = doCRC(buf,frame_max_size,0)
    buf+=chr((crcResault >> 8) & 0xff)

    if frameError:
        print("Error")
        buf += chr((crcResault+1)&0xff)
        frameError = False
    else:
        print("right")
        buf += chr(crcResault & 0xff)

    sock.sendto(buf.encode(), addr)

inputpath = "in.txt"
outputpathString = "out.txt"

def setinputpath(input):
    global inputpath
    inputpath = input

def setoutputpathString(output):
    global outputpathString
    outputpathString = output

fp = 0
fpout = 0
def from_network_layer(p):
    global fp
    global networkMessage
    if  fp == 0:
        fp = open(inputpath,'r')

    p.data = fp.read(MAX_PKT);
    if len( p.data) < MAX_PKT:
        networkMessage = False
        fp.close()

def to_network_layer(p):
    global fpout
    global outputpathString

    print("++++++++++++++++++++++++++++",outputpathString)
    if fpout == 0:
        fpout = open(outputpathString, 'w')
    else:
        fpout = open(outputpathString, 'a')
    fpout.write(''.join(p.data))
    fpout.close()
def start_timer(k):
    timer[k] = 0;
def stop_timer(k):
    timer[k] = timer_max;
def add_timer():
    global timerEnd
    for i in range(MAX_SEQ +1):
        if timer[i] == timer_max-1:
            timerEnd = True
            timer[i] = timer_max
        elif timer[i]<timer_max:
            timer[i]+=1


def enable_network_layer():
    global networkEnable
    networkEnable = True


def disable_network_layer():
    global networkEnable
    networkEnable = False

def between(a,b,c):
    if ((a <= b) and (b<c)) or ((c<a) and (a <= b)) or((b < c) and (c<a)):
        return True
    return False

def send_ack(frame_expected):
    s = frame()
    s.seq = MAX_SEQ+1
    s.ack = (frame_expected + MAX_SEQ) % (MAX_SEQ + 1)
    s.kind = frame_kind.ack
    to_physical_layer(s)

def send_data(frame_nr,frame_expected,buffer):
    s = frame()
    s.info = buffer[frame_nr]
    s.seq = frame_nr
    s.ack = (frame_expected + MAX_SEQ) % (MAX_SEQ + 1)
    s.kind = frame_kind.data
    print("seq",s.seq,"ack",s.ack)
    to_physical_layer(s)
    start_timer(frame_nr)


def protocal5():
    r = frame()
    buffer = [];
    for i in range(MAX_SEQ+1):
        buffer.append(packet())
    event = event_type.cksum_err;

    enable_network_layer();
    ack_expected = 0;
    next_frame_to_send = 0;
    frame_expected = 0;
    nbuffered = 0;

    while (True):
        event = wait_for_event()
        if event == event_type.network_layer_ready:
            print("send:\nack_expected:%d, next_frame_to_send:%d, frame_expected:%d"%(ack_expected, next_frame_to_send, frame_expected))
            from_network_layer(buffer[next_frame_to_send])
            nbuffered = nbuffered + 1
            send_data(next_frame_to_send, frame_expected, buffer)
            next_frame_to_send = inc(next_frame_to_send)
        elif event == event_type.frame_arrival:
            r = from_physical_layer()
            print("arrival:\nseq:%d, ack:%d, frame_expected:%d"%(r.seq, r.ack, frame_expected));
            if r.seq == frame_expected:
                to_network_layer(r.info)
                frame_expected = inc(frame_expected)
            if not networkMessage:
                send_ack(frame_expected)
            while between(ack_expected, r.ack, next_frame_to_send):
                nbuffered = nbuffered - 1
                stop_timer(ack_expected)
                ack_expected = inc(ack_expected)
        elif event == event_type.cksum_err:
            pass
        elif event == event_type.timeout:
            next_frame_to_send = ack_expected
            for i in range(nbuffered):
                print("timeout:\nframenum%d, ack_expected:%d, next_frame_to_send:%d"%(next_frame_to_send, ack_expected, next_frame_to_send));
                send_data(next_frame_to_send, frame_expected, buffer)
                next_frame_to_send = inc(next_frame_to_send)
        if nbuffered < MAX_SEQ:
            enable_network_layer()
        else:
            disable_network_layer()



