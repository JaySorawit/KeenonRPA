import socket
import time

def start_server():
    server_socket = socket.socket(socket.AF_INET, socket.SOCK_STREAM)
    server_socket.setsockopt(socket.SOL_SOCKET, socket.SO_REUSEADDR, 1) # Allow port reuse
    server_socket.bind(('0.0.0.0', 12345))  # Listen on all available interfaces
    server_socket.listen(1)
    
    client_socket = None  # Track client socket
    addr = None  # Store client address

    print("Server started, waiting for connection...")

    while True:
        if client_socket is None:  # If no client is connected
            try:
                client_socket, addr = server_socket.accept()
                print(f"Connected to Android device at {addr}")
                handle_client(client_socket)

            except Exception as e:
                print(f"Error in connection: {e}")
            finally:
                print("Client disconnected, waiting for new connection...")
                client_socket = None  # Reset client socket for next connection

        else:  # If a client is already connected
            print(f"Already connected to Android device at {addr}")
            handle_client(client_socket)

def handle_client(client_socket):
    try:
        while True:
            command = input("Enter command: ")
            
            if command.lower() == 'done':
                print("Exiting command sending.")
                break  # Exit the loop if 'done' is typed
            
            if command.strip() == "":  # Skip empty commands
                print("Please enter a valid command.")
                continue

            # Send the command to the robot
            client_socket.sendall((command + '\n').encode())
            print(f"Sent command: {command}")

            # Wait for acknowledgment from Android
            response = client_socket.recv(1024).decode()
            if response:
                print(f"Response from Android: {response}")

            time.sleep(1)  # Short delay between commands

    except Exception as e:
        print(f"Error handling client: {e}")

if __name__ == "__main__":
    start_server()
