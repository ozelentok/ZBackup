import Socket
import configparser
import os
import socket
import traceback
import errno

from ProtocolCodes import ErrorCode
from Logger import Logger
from SyncSession import SyncSession


class SyncServer:
    def __init__(self, config_file_path):
        self.init_config(config_file_path)
        self.init_logger()
        self.init_backup_dirs()

    def init_config(self, config_file_path):
        config_parser = configparser.ConfigParser()
        config_parser.read(config_file_path)
        self._config = dict((x, y) for x, y in config_parser.items('Server'))
        self._config['users'] = self._config['users'].split(',')
        self._config['port'] = int(self._config['port'])
        self._config['timeout'] = int(self._config['timeout'])

    def init_logger(self):
        log_file_path = self._config['log_file']
        self._logger = Logger(log_file_path)

    def init_backup_dirs(self):
        for user in self._config['users']:
            try:
                user_dir = os.path.join(self._config['root_file_dir'], user)
                os.mkdir(user_dir)
            except OSError as e:
                if e.errno != errno.EEXIST:
                    raise

    def bind(self):
        host = self._config['host']
        port = self._config['port']
        cert_file = self._config['cert_file']
        key_file = self._config['key_file']
        self.server_socket = Socket.ServerSocket(cert_file, key_file)
        self.server_socket.bind(host, port, 1)
        self._logger.log("Listening on {}:{}".format(host, port))

    def listen(self):
        try:
            while True:
                conn, addr = self.server_socket.accept()
                self.handle_new_connection(conn, addr)
        except KeyboardInterrupt:
            self._logger.log("Server Terminated")

    def validate_user(self, conn):
        user = conn.recv().decode('utf-8')
        password = conn.recv().decode('utf-8')
        if user in self._config['users'] and \
                password == self._config['password']:
            user_dir = os.path.join(self._config['root_file_dir'], user)
            conn.send_code(ErrorCode.ok)
            return user_dir
        conn.send_code(ErrorCode.fail)
        return False

    def handle_new_connection(self, conn, addr):
        try:
            self._logger.log("New connection from {}:{}".format(*addr))
            user_dir = self.validate_user(conn)
            if not user_dir:
                self._logger.log("Incorrect user and password")
                return

            self._logger.log("User validated")
            timeout = self._config['timeout']
            with SyncSession(conn, user_dir, timeout, self._logger) as session:
                session.handle_requests()

        except socket.timeout as e:
            self._logger.log("Timeout Error {}".format(e))
            traceback.print_exc()
        except socket.error as e:
            self._logger.log("Socket Error: {}".format(e))
            traceback.print_exc()
        except Exception as e:
            self._logger.log("General Error: {}".format(e))
            traceback.print_exc()
