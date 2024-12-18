import socket
import time

def start_server():
    server_socket = socket.socket(socket.AF_INET, socket.SOCK_STREAM)
    server_socket.setsockopt(socket.SOL_SOCKET, socket.SO_REUSEADDR, 1)  # Allow port reuse
    server_socket.bind(('0.0.0.0', 12345))  # Listen on all available interfaces
    server_socket.listen(1)

    print("Server started, waiting for connection...")

    while True:
        client_socket, addr = None, None
        try:
            client_socket, addr = server_socket.accept()
            print(f"Connected to Android device at {addr}")
            handle_client(client_socket)
        except Exception as e:
            print(f"Error in connection: {e}")
        finally:
            if client_socket:
                print("Closing connection with client.")
                client_socket.close()  # Ensure socket is closed after use

def receive_large_response(sock):
    sock.settimeout(10)  # Set timeout to avoid infinite hanging
    full_response = []
    try:
        while True:
            chunk = sock.recv(4096).decode('utf-8')
            if not chunk:
                print("Connection closed by client.")
                break
            if chunk.strip() == "[END]":  # Signal that all chunks are received
                break
            full_response.append(chunk)
    except socket.timeout:
        print("Socket timeout reached. No data received.")
    return ''.join(full_response)

def handle_client(client_socket):
    try:
        while True:
            command = input("Enter command: ").strip()

            if not command:  # Skip empty inputs
                print("Please enter a valid command.")
                continue

            if command.lower() == 'done':  # Exit the session
                print("Exiting command sending.")
                break

            # Send the command to the client
            client_socket.sendall((command + '\n').encode())

            if command == "getFullUI":  # Handle large responses
                print("Waiting for full response...")
                response = receive_large_response(client_socket)
                print("Full Response Received:\n", response)

            else:  # Handle regular commands
                response = client_socket.recv(4096).decode('utf-8')
                if not response:
                    print("No response received.")
                    break
                print("Response:", response)

            time.sleep(1)  # Short delay between commands

    except Exception as e:
        print(f"Error handling client: {e}")
    finally:
        print("Closing client socket.")
        client_socket.close()  # Ensure the socket is closed properly

if __name__ == "__main__":
    start_server()
