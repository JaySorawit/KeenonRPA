import socket

server_ip = "192.168.1.41"  # Replace with your server IP
port = 12345

try:
    client_socket = socket.socket(socket.AF_INET, socket.SOCK_STREAM)
    client_socket.connect((server_ip, port))
    print("Connected to the server")
except Exception as e:
    print(f"Failed to connect: {e}")
finally:
    client_socket.close()
