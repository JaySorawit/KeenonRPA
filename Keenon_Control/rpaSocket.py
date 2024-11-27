import socket
import time

def start_server():
    server_socket = socket.socket(socket.AF_INET, socket.SOCK_STREAM)
    server_socket.bind(('0.0.0.0', 12345))  # ใช้ IP ของ Raspberry Pi หรือ '0.0.0.0' เพื่อฟังทุก IP
    server_socket.listen(1)
    print("Waiting for connection...")

    while True:
        client_socket, addr = server_socket.accept()
        print(f"Connected to {addr}")

        try:
            while True:
                # วนลูปการส่งคำสั่งไปยัง Android
                commands = ["Power", "Go Charge Now", "Stop", "OK"]
                while input():
                    for command in commands:
                        client_socket.send((command + '\n').encode())  # ส่งข้อมูลพร้อม newline
                        print(f"Sent command: {command}")

                        # รับข้อมูลจาก Android (ถ้ามีการตอบกลับ)
                        # response = client_socket.recv(1024).decode()
                        # if response:
                        #     print(f"Received response from Android: {response}")

                        # รอ 5 วินาทีก่อนส่งคำสั่งถัดไป
                        time.sleep(3)

        except Exception as e:
            print(f"Error: {e}")
        finally:
            client_socket.close()
            print("Connection closed")

if __name__ == "__main__":
    start_server()
