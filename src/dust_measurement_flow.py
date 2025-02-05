from solair_measure import start_particle_measurement, stop_particle_measurement
from solair_read import read_measurement_data, client as modbus_client
from robot_rpa import send_command_to_rpa, start_server
import time
import socket
import sqlite3
import threading

# ค่ามาตรฐานของฝุ่น (ตัวอย่างกำหนดเป็น 50)
DUST_THRESHOLD = 50
MAX_RETRIES = 3

# จุดที่ต้องไปวัดฝุ่น (ตัวอย่าง)
measurement_points = ["001", "002", "003"]

def move_to_point(point):
    print(f"Moving to measurement spot {point}...")
    send_command_to_rpa("goHome")
    send_command_to_rpa("Peanut")
    send_command_to_rpa("clickBackButton")
    send_command_to_rpa("measuringSpot")
    send_command_to_rpa(point)
    time.sleep(2)  # จำลองเวลาที่ใช้ในการเคลื่อนที่

def check_dust_level():
    data = read_measurement_data()
    if data:
        dust_level = data[0]  # สมมติว่าค่าฝุ่นอยู่ที่ index 0
        print(f"Measured dust level: {dust_level}")
        return dust_level
    return None

def send_data_to_database(point, dust_level):
    conn = sqlite3.connect("dust_measurements.db")
    cursor = conn.cursor()
    cursor.execute("CREATE TABLE IF NOT EXISTS measurements (point TEXT, dust_level REAL, timestamp DATETIME DEFAULT CURRENT_TIMESTAMP)")
    cursor.execute("INSERT INTO measurements (point, dust_level) VALUES (?, ?)", (point, dust_level))
    conn.commit()
    conn.close()
    print(f"Data saved: Point {point}, Dust Level {dust_level}")

def check_solar_connection():
    """
    ตรวจสอบการเชื่อมต่อกับ SOLAIR 1100LD ก่อนที่จะส่งคำสั่งวัด
    """
    print("Checking connection to SOLAIR 1100LD...")
    try:
        if modbus_client.connect():
            print("Connected to SOLAIR 1100LD.")
            modbus_client.close()
            return True
        else:
            print("ไม่สามารถเชื่อมต่อกับ SOLAIR 1100LD ได้.")
            return False
    except Exception as e:
        print(f"Error connecting to SOLAIR 1100LD: {e}")
        return False

def measure_at_point(point):
    # ตรวจสอบการเชื่อมต่อกับ SOLAIR ก่อนสั่งวัด
    if not check_solar_connection():
        print("Skipping measurement due to SOLAIR connection issue.")
        return False

    for attempt in range(1, MAX_RETRIES + 1):
        print(f"Attempt {attempt}: Starting measurement...")
        start_particle_measurement()
        time.sleep(60)  # รอให้วัดเสร็จ
        dust_level = check_dust_level()
        if dust_level is not None:
            send_data_to_database(point, dust_level)  # บันทึกค่าฝุ่นลง database
            if dust_level <= DUST_THRESHOLD:
                print("Dust level is within acceptable range.")
                return True
        print("Dust level exceeded threshold. Retrying...")
    print("Max retries reached. Moving to the next point.")
    return False

def wait_for_rpa_connection():
    print("Waiting for RPA connection...")
    while True:
        # ส่ง ping ไปยัง RPA server
        response = send_command_to_rpa("ping")
        if response:
            print("RPA connection established.")
            return
        time.sleep(2)  # รอ 2 วินาที แล้วลองใหม่

def start_rpa_server_in_thread():
    # รัน RPA Server ใน thread แยกออกมาเพื่อให้ main thread ทำงานต่อได้
    server_thread = threading.Thread(target=start_server, daemon=True)
    server_thread.start()

def main():
    # เริ่ม RPA Server ใน thread แยก
    start_rpa_server_in_thread()
    # รอจนกว่าการเชื่อมต่อ RPA จะพร้อม
    wait_for_rpa_connection()
    
    # เมื่อ RPA เชื่อมต่อได้แล้ว เริ่มกระบวนการวัดฝุ่นในแต่ละจุด
    for point in measurement_points:
        move_to_point(point)
        # measure_at_point(point)
    print("All measurement points completed.")

if __name__ == "__main__":
    main()
