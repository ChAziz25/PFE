from kafka import KafkaConsumer, KafkaProducer
import subprocess
import json

#connect to kafka
consumer = KafkaConsumer(
    'commands',
    bootstrap_servers='localhost:9092',
    value_deserializer=lambda m: json.loads(m.decode('utf-8')),
    auto_offset_reset='latest',
    group_id='python-worker',
)

producer = KafkaProducer(
    bootstrap_servers='localhost:9092',
    value_serializer=lambda v: json.dumps(v).encode('utf-8')
)

def execute(container_ID, command):
    try:
        result = subprocess.run(
            ["docker", "exec", container_ID, command],
            capture_output=True,
            text=True
        )
        return (result.stdout + result.stderr).strip()
    except Exception as e:
        return f"Error: {str(e)}"

print("Worker started")

for message in consumer:
    data = message.value or {}
    print("RAW MESSAGE:", data)

    container_ID = data.get("targetContainerId")
    command_ID = data.get("commandId")
    command = data.get("command")

    print(f"> Executing command {command} on container {container_ID}")

    output = execute(container_ID, command)

    response = {
        "commandId": command_ID,
        "output": output,
    }

    print("RESPONSE:", response)

    producer.send('results', response)