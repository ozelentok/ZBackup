
class RequestType(object):
    download = 1
    exit = 0
    upload = 2

class ErrorCode(object):
    fail = 1
    fileexists = 4
    ok = 0
    wrongtype = 2
    invalidpath = 3

class FileType(object):
    directory = 1
    file = 0


