import os
import tempfile
from ProtocolCodes import *

class SyncSession(object):
    def __init__(self, conn, user_dir, timeout, logger):
        self.conn = conn
        self.user_dir = user_dir
        self.conn.settimeout(timeout)
        self.logger = logger

    def close(self):
        self.conn.close()

    def handle_requests(self):
        request_type = self.conn.recv_code()
        while request_type is not RequestType.exit.value:
            if request_type is RequestType.upload.value:
                self.handle_upload_request()
            else:
                self.handle_unimplemented()
            request_type = self.conn.recv_code()
        self.logger.log("Closing Connection")

    def handle_upload_request(self):
        file_type = self.conn.recv_code()
        file_path = self.transform_file_path(self.conn.recv())
        if file_path is None:
            self.conn.send_code(ErrorCode.invalidpath.value)
            return
        if file_type is FileType.directory.value:
            self.logger.log(file_path)
            self.directory_handler(file_path)
        elif file_type is FileType.file.value:
            self.logger.log(file_path)
            self.file_handler(file_path)
        else:
            self.conn.send_code(ErrorCode.fail.value)

    def directory_handler(self, file_path):
        if not os.path.exists(file_path):
            try:
                os.mkdir(file_path)
                self.conn.send_code(ErrorCode.ok.value)
            except OSError as e:
                self.logger.log(e)
                self.conn.send_code(ErrorCode.fail.value)
        elif not os.path.isdir(file_path):
            self.conn.send_code(ErrorCode.wrongtype.value)
        else:
            self.conn.send_code(ErrorCode.ok.value)

    def file_handler(self, file_path):
        file_time = self.conn.recv_long_code() / 1000;
        if not os.path.exists(file_path):
            self.conn.send_code(ErrorCode.ok.value)
            self.write_uploaded_file(file_path, file_time)
        elif os.path.isdir(file_path):
            self.conn.send_code(ErrorCode.wrongType.value)
        elif self.are_files_equal(file_path, file_time):
            self.conn.send_code(ErrorCode.fileexists.value)
        else:
            self.conn.send_code(ErrorCode.ok.value)
            self.write_uploaded_file(file_path, file_time)

    def are_files_equal(self, file_path, remote_file_time):
        local_file_time = os.path.getmtime(file_path);
        return (local_file_time == remote_file_time)

    def transform_file_path(self, file_path):
        file_path = file_path.decode('utf-8')
        if '..' in file_path:
            return None
        if file_path[0] is '/':
            file_path = file_path[1:]
        file_path = file_path.replace('/', os.path.sep)
        return os.path.join(self.user_dir, file_path)

    def handle_unimplemented(self):
        self.conn.send_code(ErrorCode.fail.value)

    def write_uploaded_file(self, file_path, file_time):
        file_size = self.conn.recv_long_code();
        current_size = 0
        try:
            temp_file, temp_file_path = tempfile.mkstemp(
                    prefix='zbackup.tmp',
                    dir=os.path.dirname(file_path))
            temp_file = os.fdopen(temp_file, 'wb')
            while current_size < file_size:
                buf = self.conn.recv()
                current_size += len(buf)
                temp_file.write(buf)
            temp_file.close()
            os.rename(temp_file_path, file_path)
            os.utime(file_path, (file_time, file_time))
            self.conn.send_code(ErrorCode.ok.value)
        except IOError as e:
            self.logger.log(e)
            self.conn.send_code(ErrorCode.fail.value)
