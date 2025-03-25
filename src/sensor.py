from pymodbus.client import ModbusTcpClient
from pymodbus.exceptions import ModbusIOException
import time
from src import CONFIG

class Sensor:
    def __init__(self):
        self.client = ModbusTcpClient(CONFIG["SOLAIR_IP"])
    
    def check_connection(self):
        print("Checking connection to SOLAIR 1100LD...")
        if self.client.connect():
            print("Connected to SOLAIR 1100LD.")
            self.client.close()
            return True
        print("Failed to connect to SOLAIR 1100LD.")
        return False

    def start_measurement(self):
        try:
            self.client.connect()
            self.client.write_register(1, 11)  # Start command
            print("Measurement started.")
            time.sleep(70)
            self.client.write_register(1, 12)  # Stop command
            print("Measurement stopped.")
            self.client.close()
        except Exception as e:
            print(f"Measurement error: {e}")
    
    def read_data(self):

        try:
            # ???????????? Modbus TCP server
            self.client.connect()

            # ??????? Record Index (40025) ???? 22 ???????? record ??????????
            record_count = self.client.read_holding_registers(address =40024-40001,count=1)
            self.client.write_register(40025-40001,record_count.registers[0]-1)

            # ?????????? register ??????? 30xxx ?????????????
            # ??? `self.client.read_holding_registers` ???????????????????????????????????????
            register_address = 30001-30001
            response = self.client.read_input_registers(register_address, count=100)
            
            if response.isError():
                
                print("Error reading record.")
            else:
                # data = {
                #     'measurement_datetime': measurement_time.isoformat(),
                #     'room': room,
                #     'area': area,
                #     'location_name': location,
                #     'count': count,
                #     'um01': um_values['um01'],
                #     'um03': um_values['um03'],
                #     'um05': um_values['um05'],
                #     'running_state': random.randint(0, 1),
                #     'alarm_high': 0,
                # }
                
                print(f"Record values: {response.registers[9]}")
                print(f"Record values: {response.registers}")

            # ???????????????
            self.client.close()
            return data

        except Exception as e:
            print(f"Error reading data: {e}")
            return None
