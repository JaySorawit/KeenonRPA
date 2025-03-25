import requests

POST_MEASUREMENT_API = "http://localhost:5000/api/dust-measurements"

class Database:
    # POST generated mock data to the API
    def save_measurement(data):
        print(f"Preparing to post {len(data)} mock measurements.")
        for entry in data:
            response = requests.post(POST_MEASUREMENT_API, json=entry)
            if response.status_code == 201:
                print(f"Successfully created dust measurement: {entry}")
            else:
                print(f"Failed to create dust measurement: {response.status_code}, {response.text}")
                