import os
import datetime
import logging
import json

class DustLogger:
    def __init__(self):
        self.logger = None
    
    #บันทึกค่าตามวันที่  Year-Month/Date/Location/*.log
    def setup_logger(self, location):
        """ Setup logger """
        now = datetime.datetime.now()
        year_month = now.strftime("%Y-%m")
        date = now.strftime("%d")
        dir_path = os.path.join(year_month, date, location)
        os.makedirs(dir_path, exist_ok=True)
        
        log_file = os.path.join(dir_path, f"{year_month}-{date}_{location}.log")
        
        self.logger = logging.getLogger(f"DustLogger_{location}")
        self.logger.setLevel(logging.INFO)
        
        if self.logger.hasHandlers():
            self.logger.handlers.clear()
        
        handler = logging.FileHandler(log_file, encoding="utf-8")
        formatter = logging.Formatter('%(asctime)s - %(levelname)s - %(message)s')
        handler.setFormatter(formatter)
        self.logger.addHandler(handler)
    
    def save_log(self, data,count):
        """ Save log with dynamic location """
        # Set up logger only when save_log is called
        self.setup_logger(data['location'])
        data['count'] = count
        # Log data
        self.logger.info(json.dumps(data, ensure_ascii=False))