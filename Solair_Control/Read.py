from pymodbus.client import ModbusTcpClient
from pymodbus.exceptions import ModbusIOException
import time  # นำเข้าโมดูล time เพื่อใช้ในการเว้นเวลา

# กำหนด IP และพอร์ตของ SOLAIR 1100LD
client = ModbusTcpClient('IP_ADDRESS', port=502)

# Function to set Record Index for reading measurement data
def set_record_index(index=-1):
    record_index_register = 40025 - 40001  # Register for Record Index
    try:
        # Set the Record Index to -1 to get the latest value
        client.write_register(record_index_register, index)
        print("Record Index set successfully")
    except Exception as e:
        print(f"Error setting Record Index: {e}")

# Function to read measurement data from registers 30xxx
def read_measurement_data():
    try:
        # Read data from registers 30001 to 30010 (example)
        result = client.read_holding_registers(30001 - 30001, 10)
        if result.isError():
            print("Unable to read measurement data")
        else:
            print("Measurement data:", result.registers)
    except Exception as e:
        print(f"Error reading data: {e}")

# Connect to the device and read the latest measurement data
if client.connect():
    set_record_index(-1)  # Set Record Index to -1 to get the latest value
    read_measurement_data()  # Read measurement data
    client.close()
else:
    print("Unable to connect to SOLAIR 1100LD")