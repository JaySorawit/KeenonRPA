import socket
import time
import threading

def start_server():
    server_socket = socket.socket(socket.AF_INET, socket.SOCK_STREAM)
    server_socket.setsockopt(socket.SOL_SOCKET, socket.SO_REUSEADDR, 1)
    server_socket.bind(('0.0.0.0', 12345))
    server_socket.listen(5)
    print("Server started, waiting for connection...")

    while True:
        try:
            client_socket, addr = server_socket.accept()
            print(f"Connected to Android device at {addr}")
            # ใช้ threading เพื่อรองรับการเชื่อมต่อพร้อมกันหลายๆ connection
            threading.Thread(target=handle_client, args=(client_socket,), daemon=True).start()
        except Exception as e:
            print(f"Error in connection: {e}")

def send_command_to_rpa(command):
    """
    ฟังก์ชันสำหรับส่งคำสั่งไปยัง RPA server
    โดยจะส่ง handshake 'start connection rpa' ก่อนส่งคำสั่งจริง
    """
    try:
        client = socket.socket(socket.AF_INET, socket.SOCK_STREAM)
        client.connect(('127.0.0.1', 12345))
        # ส่ง handshake
        client.sendall(("start connection rpa\n").encode())
        handshake_resp = client.recv(1024).decode('utf-8')
        # จากนั้นส่งคำสั่งจริง
        client.sendall((command + '\n').encode())
        response = client.recv(4096).decode('utf-8')
        client.close()
        return response
    except Exception as e:
        print(f"Error sending command to RPA: {e}")
        return None

def handle_client(client_socket):
    try:
        # อ่าน handshake เริ่มต้นจาก client
        initial_command = client_socket.recv(1024).decode('utf-8').strip()
        if initial_command != "start connection rpa":
            print("Invalid initial command from client. Closing connection.")
            client_socket.close()
            return

        # ส่งการตอบรับ handshake
        client_socket.sendall("Handshake accepted\n".encode())
        print("RPA connection established. Waiting for commands...")

        while True:
            command = client_socket.recv(1024).decode('utf-8').strip()
            if not command:
                break
            if command.lower() == 'done':
                print("Exiting session.")
                break
            print(f"Executing command: {command}")
            # ตัวอย่าง: ส่ง echo กลับไป
            client_socket.sendall(("Command received: " + command + "\n").encode())
    except Exception as e:
        print(f"Error handling client: {e}")
    finally:
        print("Closing client socket.")
        client_socket.close()

if __name__ == "__main__":
    start_server()
