class RequestType:
    exit = 0
    download = 1
    upload = 2


class ErrorCode:
    ok = 0
    fail = 1
    wrongtype = 2
    invalidpath = 3
    fileexists = 4


class FileType:
    file = 0
    directory = 1
