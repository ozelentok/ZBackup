import socket
import ssl
import struct

class ServerSocket(object):
    def __init__(self, certfile, keyfile):
        plain_socket = socket.socket(socket.AF_INET, socket.SOCK_STREAM)
        plain_socket.setsockopt(socket.SOL_SOCKET, socket.SO_REUSEADDR, 1)
        self.socket = ssl.wrap_socket(plain_socket,
                                        server_side=True,
                                        certfile=certfile,
                                        keyfile=keyfile,
                                        ssl_version=ssl.PROTOCOL_TLSv1)

    def bind(self, host, port, max_connections):
        self.socket.bind((host, port))
        self.socket.listen(max_connections)

    def accept(self):
        conn, addr = self.socket.accept()
        conn.settimeout(self.socket.gettimeout())
        return Socket(conn), addr

    def close(self):
        self.socket.close()

class Socket(object):
    def __init__(self, base_socket):
        self.socket = base_socket

    def settimeout(self, value):
        self.socket.settimeout(value)

    def gettimeout(self):
        return self.socket.gettimeout()

    def send(self, bytes_to_send):
        self.send_code(len(bytes_to_send))
        self.socket.send(bytes_to_send)

    def recv(self):
        length = self._recv_length()
        return self._recv_bytes(length)

    def send_code(self, code):
        self.socket.send(struct.pack('!l', code))

    def recv_code(self):
        return self._recv_length()

    def send_long_code(self, code):
        self.socket.send(struct.pack('!q', code))

    def recv_long_code(self):
        length_size = 8
        length_buffer = b''
        while len(length_buffer) < length_size:
            bytes_recv = self.socket.recv(length_size - len(length_buffer))
            if bytes_recv == None:
                raise socket.error('Connection disconnected')
            length_buffer += bytes_recv
        return struct.unpack('!q', length_buffer)[0]


    def _recv_length(self):
        length_size = 4
        length_buffer = b''
        while len(length_buffer) < length_size:
            bytes_recv = self.socket.recv(length_size - len(length_buffer))
            if bytes_recv == None:
                raise socket.error('Connection disconnected')
            length_buffer += bytes_recv
        return struct.unpack('!l', length_buffer)[0]

    def _recv_bytes(self, length):
        bytes_buffer = b''
        while len(bytes_buffer) < length:
            bytes_recv = self.socket.recv(length - len(bytes_buffer))
            if bytes_recv == None:
                raise socket.error('Connection disconnected')
            bytes_buffer += bytes_recv
        return bytes_buffer

    def close(self):
        self.socket.close()
