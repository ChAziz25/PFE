from kafka import KafkaConsumer, KafkaProducer
import subprocess
import json

#connect to kafka
consumer = KafkaConsumer(
    'commands',
    bootstrap_servers='localhost:9092',
    value_deserializer=lambda m: json.loads(m.decode('utf-8')),
    auto_offset_reset='latest',
    enable_auto_commit=True,
    group_id='python-worker',
)

producer = KafkaProducer(
    bootstrap_servers='localhost:9092',
    value_serializer=lambda v: json.dumps(v).encode('utf-8')
)

def execute(container_ID, command):
    try:
        result = subprocess.run(
            ["docker", "exec", container_ID, "sh", "-c", command],
            capture_output=True,
            text=True,timeout=30
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

    future = producer.send('results', response)
    future.get(timeout=10)