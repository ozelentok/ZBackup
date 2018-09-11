#!/usr/bin/env python3

import SyncServer


def main():
    server = SyncServer.SyncServer('server.ini')
    server.bind()
    server.listen()


if __name__ == '__main__':
    main()
