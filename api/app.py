from fastapi import FastAPI
from pydantic import BaseModel
from src import CONFIG, Database, Robot, Sensor
import time

app = FastAPI()
robot = Robot()
sensor = Sensor()
db = Database()

points = []
# max_retries = CONFIG["MAX_RETRIES"]
# ucl_limit = CONFIG["UCL_LIMIT"]

# offline_measurements = []  
#robot.start_server()

# Define request model
class PointRequest(BaseModel):
    point: str
    
@app.post("/send-point")
async def send_point(data: PointRequest):
    point = data.point
    points.append(point)
    print(f"Added {point} to the queue.")
    return {"message": points}

#app get points
@app.get("/get-points")
async def get_points():
    if len(points) > 0:
        print(f"User get {points}")
        return {"points": points}
    else:
        return {"points": None}
    
@app.get("/go")
async def main():
    if len(points) == 0:
        return {"message": "No points to go."}
    
    robot.send_command("goHome")
    robot.send_command("Peanut")

    for point in points:
        robot.send_command("clickBackButton")
        robot.send_command("Direct")
        robot.send_command(point)
        robot.send_command("Go")
        
        # Wait for the robot to finish the task

       
if __name__ == "__main__":
    import uvicorn
    uvicorn.run(app, host="0.0.0.0", port=8000, reload=True)
