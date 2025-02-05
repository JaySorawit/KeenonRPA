from pymodbus.client import ModbusTcpClient
from pymodbus.exceptions import ModbusIOException
import time  # นำเข้าโมดูล time เพื่อใช้ในการเว้นเวลา

# กำหนด IP และพอร์ตของ SOLAIR 1100LD
client = ModbusTcpClient('IP_ADDRESS', port=502)

# ฟังก์ชันสำหรับเริ่มการวัดฝุ่น
def start_particle_measurement(mode=11):
    command_register = 40002 - 40001  # รีจิสเตอร์สำหรับคำสั่ง (ใช้ 0-based index)
    
    try:
        # เขียนคำสั่งเพื่อเริ่มการวัดไปยัง Command Register
        result = client.write_register(command_register, mode)
        if isinstance(result, ModbusIOException):
            print("การสั่งเริ่มวัดไม่สำเร็จ")
        else:
            print("เริ่มต้นการวัดสำเร็จ")
    except Exception as e:
        print(f"เกิดข้อผิดพลาด: {e}")

# ฟังก์ชันสำหรับหยุดการวัดฝุ่น
def stop_particle_measurement(mode=12):
    command_register = 40002 - 40001  # รีจิสเตอร์สำหรับคำสั่ง (ใช้ 0-based index)
    
    try:
        # เขียนคำสั่งเพื่อหยุดการวัดไปยัง Command Register
        result = client.write_register(command_register, mode)
        if isinstance(result, ModbusIOException):
            print("การสั่งหยุดวัดไม่สำเร็จ")
        else:
            print("หยุดการวัดสำเร็จ")
    except Exception as e:
        print(f"เกิดข้อผิดพลาด: {e}")

# เชื่อมต่อกับเครื่องและสั่งเริ่ม/หยุดการวัด
if client.connect():
    start_particle_measurement(mode=11)  # เริ่มการวัดด้วย Instrument Start (mode=11)
    time.sleep(60)  # รอ 60 วินาทีเพื่อให้การวัดดำเนินการ
    stop_particle_measurement(mode=12)  # หยุดการวัดด้วย Instrument Stop (mode=12)
    client.close()
else:
    print("ไม่สามารถเชื่อมต่อกับ SOLAIR 1100LD ได้")
