from pymodbus.client import ModbusTcpClient
from pymodbus.exceptions import ModbusIOException
import time

client = ModbusTcpClient('IP_ADDRESS', port=502)

def set_record_index(index=-1):
    record_index_register = 40025 - 40001
    try:
        client.write_register(record_index_register, index)
        print("Record Index set successfully")
    except Exception as e:
        print(f"Error setting Record Index: {e}")

def read_measurement_data():
    try:
        result = client.read_holding_registers(30001 - 30001, 10)
        if result.isError():
            print("Unable to read measurement data")
            return None
        print("Measurement data:", result.registers)
        return result.registers
    except Exception as e:
        print(f"Error reading data: {e}")
        return None

if __name__ == "__main__":
    if client.connect():
        set_record_index(-1)
        read_measurement_data()
        client.close()
    else:
        print("Unable to connect to SOLAIR 1100LD")
