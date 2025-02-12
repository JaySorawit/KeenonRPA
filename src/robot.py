import socket
import time
import threading
from src import CONFIG

class Robot:
    def __init__(self):
        self.server_ip = CONFIG["RPA_IP"]
        self.server_port = CONFIG["RPA_PORT"]
        self.server_thread = None
        self.server_socket = None
        self.client_socket = None

    def start_server(self):
        """ เริ่มต้น Socket Server เพื่อรับคำสั่งจาก Android """
        self.server_socket = socket.socket(socket.AF_INET, socket.SOCK_STREAM)
        self.server_socket.setsockopt(socket.SOL_SOCKET, socket.SO_REUSEADDR, 1)
        self.server_socket.bind(('0.0.0.0', self.server_port))
        self.server_socket.listen(1)
        print("Server started, waiting for connection...")

        while (self.client_socket == None):
            try :
                self.client_socket, addr = self.server_socket.accept()
                print(f"Connected to Android device at {addr}")
                print("client:", self.client_socket)
                # ใช้ threading เพื่อรองรับหลาย connection
                #threading.Thread(target=self._handle_client, args=(client_socket,), daemon=True).start()
                # self.handle_client(self.client_socket)
            except Exception as e:
                print(f"Error in connection: {e}")

    def receive_large_response(self):
        self.client_socket.settimeout(10)  # Set timeout to avoid infinite hanging
        full_response = []
        try:
            while True:
                chunk = self.client_socket.recv(4096).decode('utf-8')
                if not chunk:
                    print("Connection closed by client.")
                    break
                if chunk.strip() == "[END]":  # Signal that all chunks are received
                    break
                full_response.append(chunk)
        except socket.timeout:
            print("Socket timeout reached. No data received.")
        return ''.join(full_response)
    
    def send_command(self, command):
        # commands = ["goHome, Peanut App", "checkStatus",]  # Predefined commands
        try:
            print(f"Sending command: {command}")
            self.client_socket.sendall((command + '\n').encode())

            if command == "getFullUI":  # Handle large responses
                print("Waiting for full response...")
                response = self.receive_large_response()
                print("Full Response Received:\n", response)
            else:  # Handle regular commands
                response = self.client_socket.recv(4096).decode('utf-8')
                if not response:
                    print("No response received.")
                print("Response:", response)

            time.sleep(2)  # Delay between sending commands

        except Exception as e:
            print(f"Error handling client: {e}")
            # finally:
            #     print("Closing client socket.")
            #     self.client_socket.close()  # Ensure the socket is closed properly

    # def _handle_client(self, client_socket):
    #     """ จัดการ Connection จาก Client """
    #     try:
    #         initial_command = client_socket.recv(1024).decode('utf-8').strip()
    #         if initial_command != "start connection rpa":
    #             print("Invalid initial command from client. Closing connection.")
    #             return

    #         client_socket.sendall("Handshake accepted\n".encode())
    #         print("RPA connection established. Waiting for commands...")

    #         while True:
    #             command = client_socket.recv(1024).decode('utf-8').strip()
    #             if not command:
    #                 break
    #             if command.lower() == 'done':
    #                 print("Exiting session.")
    #                 break
    #             print(f"Executing command: {command}")
    #             client_socket.sendall(f"Command received: {command}\n".encode())

    #     except Exception as e:
    #         print(f"Error handling client: {e}")
    #     finally:
    #         print("Closing client socket.")
    #         client_socket.close()

    # def start_rpa_server_in_thread(self):
    #     """ รัน RPA Server ใน Thread แยกเพื่อไม่ให้บล็อคโปรแกรมหลัก """
    #     self.server_thread = threading.Thread(target=self.start_server, daemon=True)
    #     self.server_thread.start()

    # def wait_for_rpa_connection(self):
    #     """ รอให้ RPA Server พร้อมใช้งาน โดยมี timeout """
    #     print("Waiting for RPA connection...")
        
        
    #     while self.client_socket == None:
    #         if self.handle_client(["ping"]):
    #             print("RPA connection established.")
    #             return True
    #         time.sleep(2)

    #     print("Timeout: RPA server is not responding.")
    #     return False

    # def send_command_to_rpa(self, command):
    #     """ ส่งคำสั่งไปยัง RPA server พร้อม handshake """
    #     try:
    #         client = socket.socket(socket.AF_INET, socket.SOCK_STREAM)
    #         client.settimeout(5)  # ป้องกันการค้าง
    #         client.connect((self.server_ip, self.server_port))
    #         client.sendall("start connection rpa\n".encode())

    #         handshake_resp = client.recv(1024).decode('utf-8')
    #         if "Handshake accepted" not in handshake_resp:
    #             print("Handshake failed.")
    #             client.close()
    #             return None

    #         client.sendall(f"{command}\n".encode())
    #         response = client.recv(4096).decode('utf-8')
    #         print(f"Send command: {command}, Response: {response}")

    #         client.close()
    #         return response
    #     except socket.timeout:
    #         print("Timeout error: Unable to reach RPA server.")
    #         return None
    #     except Exception as e:
    #         print(f"Error sending command to RPA: {e}")
    #         return None

    # def move_to_point(self, point):
    #     """ สั่งให้ RPA เคลื่อนที่ไปยังตำแหน่งที่กำหนด """
    #     print(f"Moving to measurement spot {point}...")
    #     commands = ["goHome", "Peanut", "clickBackButton", "measuringSpot", point]
    #     for command in commands:
    #         self.send_command_to_rpa(command)
    #         time.sleep(1) # wait for click
    #     time.sleep(3) # wait for robot moving
