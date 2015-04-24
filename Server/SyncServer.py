import Socket
import ConfigParser
import os
import socket
import traceback
import errno
from ProtocolCodes import *
from Logger import Logger
from SyncSession import SyncSession

class SyncServer(object):

    def __init__(self, config_file_path, log_file_path):
        self.init_config(config_file_path)
        self.logger = Logger(log_file_path)
        self.init_backup_dirs()

    def init_config(self, config_file_path):
        config_parser = ConfigParser.ConfigParser()
        config_parser.read(config_file_path)
        self.config = dict((x, y) for x, y in config_parser.items('Server'))
        self.config['users'] = self.config['users'].split(',')
        self.config['port'] = int(self.config['port'])
        self.config['timeout'] = int(self.config['timeout'])

    def init_backup_dirs(self):
        for user in self.config['users']:
            try:
                user_dir = os.path.join(self.config['root_file_dir'], user)
                os.mkdir(user_dir)
            except OSError as e:
                if e.errno != errno.EEXIST:
                    raise

    def bind(self):
        host = self.config['host']
        port = self.config['port']
        cert_file = self.config['cert_file']
        key_file = self.config['key_file']
        self.server_socket = Socket.ServerSocket(cert_file, key_file)
        self.server_socket.bind(host, port, 1)
        self.logger.log("Listening on {}:{}".format(host, port))

    def listen(self):
        try:
            while True:
                conn, addr = self.server_socket.accept()
                self.handle_new_connection(conn, addr)
        except KeyboardInterrupt:
            self.logger.log("Server Terminated")

    def validate_user(self, conn):
        user = conn.recv().decode('utf-8')
        password = conn.recv().decode('utf-8')
        if user in self.config['users'] and password == self.config['password']:
            user_dir = os.path.join(self.config['root_file_dir'], user)
            conn.send_code(ErrorCode.ok.value)
            return user_dir
        conn.send_code(ErrorCode.fail.value)
        return False

    def handle_new_connection(self, conn, addr):
        try:
            self.logger.log("New connection from {}:{}".format(*addr))
            user_dir = self.validate_user(conn)
            if user_dir:
                self.logger.log("User validated")
                session = SyncSession(conn, user_dir, self.config['timeout'], self.logger)
                session.handle_requests()
            else:
                self.logger.log("Incorrect user and password")

        except socket.timeout as e:
            self.logger.log("Timeout Error {}".format(e))
            traceback.print_exc()
        except socket.error as e:
            self.logger.log("Socket Error: {}".format(e))
            traceback.print_exc()
        except Exception as e:
            self.logger.log("General Error: {}".format(e))
            traceback.print_exc()
        finally:
            session.close()
