import sqlite3
from src import CONFIG

class Database:
    
    def __init__(self):
        self.db_path = CONFIG["DB_PATH"]
        self._initialize_db()
    
    
    def _initialize_db(self):
        try:
            conn = sqlite3.connect(self.db_path)
            cursor = conn.cursor()
            cursor.execute(
            """
                CREATE TABLE IF NOT EXISTS measurements (
                point TEXT,
                dust_level REAL,
                count INTEGER,
                timestamp DATETIME DEFAULT CURRENT_TIMESTAMP
                )
            """)

            conn.commit()
            conn.close()
            
        except sqlite3.Error as e: 
            print(f"Database error: {e}")
            return None
        
        finally:
            if conn:
                conn.close() 
    
    
    def save_measurement(self, point, dust_level, count):
        try:
            conn = sqlite3.connect(self.db_path)
            cursor = conn.cursor()
            
            cursor.execute("INSERT INTO measurements (point, dust_level, count) VALUES (?, ?, ?)"
                            , (point, dust_level, count))
            
            conn.commit()
            conn.close()
            print(f"Saved: Point {point}, Dust Level {dust_level}")
            
        except sqlite3.Error as e: 
            print(f"Database error: {e}")
            return None

        finally:
            if conn:
                conn.close() 
    
    
    def get_measurement(self):
        try:
            conn = sqlite3.connect(self.db_path)
            cursor = conn.cursor()
            cursor.execute("SELECT * FROM measurements")
            result = cursor.fetchall() 
            #result = cursor.fetchmany(5)  # ดึงทีละ 5 แถว
            
            return result if result else None

        except sqlite3.Error as e: 
            print(f"Database error: {e}")
            return None

        finally:
            if conn:
                conn.close() 
