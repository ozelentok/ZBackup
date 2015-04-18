#!/usr/bin/env python2

import SyncServer
def main():
    server = SyncServer.SyncServer('server.ini', 'log.txt')
    server.bind()
    server.listen()

if __name__ == '__main__':
    main()
