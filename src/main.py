from src import CONFIG, Database, Robot, Sensor
import time
import random

# class Sensor:
    
#     @staticmethod
#     def check_connection():
#         return True
    
#     @staticmethod
#     def start_measurement():
#         return True
        
#     @staticmethod
#     def read_data():
#         return random.randint(80, 120)

##### flow #####
# goHome
# Peanut Food Delivery

# ซ้ายบน
# Direct
# (รูด) Loction
# Go
# เมื่อไปถึงจะกลับมามีปุ่ม Go
        
def main(): 
    robot = Robot()
    sensor = Sensor()
    db = Database()
    
    max_retries = CONFIG["MAX_RETRIES"]
    ucl_limit = CONFIG["UCL_LIMIT"]
    
    offline_measurements = []  # เก็บค่าฝุ่นเมื่อ DB เชื่อมต่อไม่ได้

    robot.start_server()

    time.sleep(10)

    # go_app = ["Direct"]
    # for c in go_app:
    #     robot.send_command(c)

    # time.sleep(10)

    points = ["CR14_R3", "CR15_R3"]


    for point in points:
        robot.send_command("Direct")
        time.sleep(10)
        robot.send_command(point)
        robot.send_command("Go")

        # Wait 
        time.sleep(60)
        #if not sensor.check_connection():
        #    print(f"Skipping {point} due to sensor connection failure.")
        #    continue
        
        count = 1
        dust_level = None
        while count < max_retries + 1:
            sensor.start_measurement()
            dust_level = sensor.read_data()
            
            if dust_level is None:
                print(f"Measurement failed at {point}. Retrying...")
            elif dust_level <= ucl_limit:
                print(f"Dust level at {point} is within range: {dust_level}")
                break  # ออกจาก loop ถ้าค่าปกติ
            else:
                print(f"Dust level at {point} exceeded UCL ({dust_level}). Retrying {count}/3...")
            
            # บันทึกค่าฝุ่น
            try:
                db.save_measurement(point, dust_level, count)
                print(f"Saved measurement at {point}: {dust_level}: {count}")
            except Exception as e:
                print(f"Database error: {e}. Storing offline.")
                offline_measurements.append((point, dust_level, count))
                
            count += 1
            time.sleep(2)
        
        #robot.send_command("OK")
        time.sleep(10)
    
    # eye
    time.sleep(10)
    #robot.send_command("Exit")
    #robot.send_command("OK")

    # พยายามส่งค่าที่ค้างอยู่ไปยัง Database
    if offline_measurements:
        print("Retrying to save offline measurements...")
        for point, dust_level in offline_measurements:
            try:
                db.save_measurement(point, dust_level, count)
                print(f"Recovered and saved {point}: {dust_level}: {count}")
                offline_measurements.remove((point, dust_level, count)) 
            except Exception as e:
                print(f"Still unable to save {point}: {e}")
    
    print("All measurements completed.")

if __name__ == "__main__":
    main()
