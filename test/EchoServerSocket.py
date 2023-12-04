# -*- coding: utf-8 -*-
"""
Created on Sat Aug 13 16:50:47 2022

@author: localuser
"""

import socket

HOST = 'localhost'                 # Symbolic name meaning all available interfaces
PORT = 9999              # Arbitrary non-privileged port
with socket.socket(socket.AF_INET, socket.SOCK_STREAM) as s:
    s.bind((HOST, PORT))
    s.listen(1)
    conn, addr = s.accept()
    with conn:
        print('Connected by', addr)
        while True:
            data = conn.recv(1024)
            if not data:
                break
            msg = data.decode("utf-8", "ignore").rstrip()
            print("Msg: "+msg)
            conn.sendall(data)
            if msg.startswith("close"):
                break
    s.close()