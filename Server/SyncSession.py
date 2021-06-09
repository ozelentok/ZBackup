import os
import tempfile
import errno

from ProtocolCodes import ErrorCode, RequestType, FileType


class SyncSession:
    def __init__(self, conn, user_dir, timeout, logger):
        self._conn = conn
        self._user_dir = user_dir
        self._conn.settimeout(timeout)
        self._logger = logger

    def __enter__(self):
        return self

    def __exit__(self, type, value, tb):
        self.close()
        return False

    def close(self):
        self._conn.close()

    def handle_requests(self):
        request_type = self._conn.recv_code()
        while request_type is not RequestType.exit:
            if request_type is RequestType.upload:
                self.handle_upload_request()
            else:
                self.handle_unimplemented()
            request_type = self._conn.recv_code()
        self._logger.log("Closing Connection")

    def handle_upload_request(self):
        file_type = self._conn.recv_code()
        file_path = self.transform_file_path(self._conn.recv())
        if file_path is None:
            self._conn.send_code(ErrorCode.invalidpath)
            return
        if file_type is FileType.directory:
            self._logger.log(file_path)
            self.directory_handler(file_path)
        elif file_type is FileType.file:
            self._logger.log(file_path)
            self.file_handler(file_path)
        else:
            self._conn.send_code(ErrorCode.fail)

    def directory_handler(self, file_path):
        if not os.path.exists(file_path):
            try:
                os.mkdir(file_path)
                self._conn.send_code(ErrorCode.ok)
            except OSError as e:
                self._logger.log(e)
                self._conn.send_code(ErrorCode.fail)
        elif not os.path.isdir(file_path):
            self._conn.send_code(ErrorCode.wrongtype)
        else:
            self._conn.send_code(ErrorCode.ok)

    def file_handler(self, file_path):
        file_time = self._conn.recv_long_code() / 1000
        if not os.path.exists(file_path):
            self._conn.send_code(ErrorCode.ok)
            self.write_uploaded_file(file_path, file_time)
        elif os.path.isdir(file_path):
            self._conn.send_code(ErrorCode.wrongType)
        elif SyncSession.are_files_equal(file_path, file_time):
            self._conn.send_code(ErrorCode.fileexists)
        else:
            self._conn.send_code(ErrorCode.ok)
            self.write_uploaded_file(file_path, file_time)

    @staticmethod
    def are_files_equal(file_path, remote_file_time):
        local_file_time = os.path.getmtime(file_path)
        return (local_file_time == remote_file_time)

    def transform_file_path(self, file_path):
        file_path = file_path.decode('utf-8')
        if '..' in file_path:
            return None
        if file_path[0] == '/':
            file_path = file_path[1:]
        file_path = file_path.replace('/', os.path.sep)
        return os.path.join(self._user_dir, file_path)

    def handle_unimplemented(self):
        self._conn.send_code(ErrorCode.fail)

    def write_uploaded_file(self, file_path, file_time):
        file_size = self._conn.recv_long_code()
        current_size = 0
        try:
            temp_file, temp_file_path = tempfile.mkstemp(
                prefix='zbackup.tmp',
                dir=os.path.dirname(file_path))
            temp_file = os.fdopen(temp_file, 'wb')
            while current_size < file_size:
                buf = self._conn.recv()
                current_size += len(buf)
                temp_file.write(buf)
            temp_file.close()
            SyncSession.rename_temp_path_to_final_path(temp_file_path,
                                                       file_path, file_time)
            self._conn.send_code(ErrorCode.ok)
        except IOError as e:
            self._logger.log(e)
            self._conn.send_code(ErrorCode.fail)

    @staticmethod
    def rename_temp_path_to_final_path(temp_path, final_path, file_time):
        try:
            os.remove(final_path)
        except OSError as e:
            if e.errno != errno.ENOENT:
                raise

        os.rename(temp_path, final_path)
        os.utime(final_path, (file_time, file_time))
