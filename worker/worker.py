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

def wait_for_ready(container_id, timeout=10):
    for _ in range(timeout):
        result = subprocess.run(
            ["docker", "inspect", "--format", "{{.State.Status}}", container_id],
            capture_output=True,
            text=True
        )
        if result.stdout.strip() == "running":
            return True
        
        time.sleep(1)

    return False

def get_container_name(container_id):
    result = subprocess.run(
        ["docker", "inspect", "-f", "{{.Name}}", container_id],
        capture_output=True,
        text=True
    )
    return result.stdout.strip().replace("/", "")


def create_container(memory, cpu):
    try:
        result = subprocess.run(
            [
                "docker", "run", "-d",
                f"--memory={memory}mb",
                f"--cpus={cpu}",
                "pfe-sandbox"
            ],
            capture_output=True,
            text=True
        )
        
        if result.returncode != 0:
            return f"Error: {result.stderr.strip()}"

        return result.stdout.strip()
    except Exception as e:
        return f"Error: {str(e)}"

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

    msg_type = data.get("type")

    if msg_type == "CREATE_CONTAINER":
        request_ID = data.get("requestId")
        memory = data.get("memory")
        cpu = data.get("cpu")
        user = data.get("userId")

        print(f"> Creating container with memory {memory} and CPU {cpu}")
        print(f"> userId received: {user}")
        container_ID = create_container(memory, cpu)

        if "Error" not in container_ID:
            wait_for_ready(container_ID)
            name = get_container_name(container_ID)
        else:
            name = None

        response = {
            "type": msg_type,
            "requestId": request_ID,
            "containerId": container_ID,
            "name": name,
            "userId": user
        }
    elif msg_type == "EXECUTE_COMMAND":
        command_ID = data.get("commandId")
        container_ID = data.get("targetContainerId")
        command = data.get("command")
        source = data.get("source")

        print(f"> Executing command {command} on container {container_ID}")
        output = execute(container_ID, command)

        response = {
            "type": msg_type,
            "commandId": command_ID,
            "output": output,
            "source": source
        }
    else:
        response = {
            "type": msg_type,
            "commandId": command_ID,
            "error": "Invalid message type"
        }

    print("RESPONSE:", response)

    future = producer.send('results', response)
    future.get(timeout=10)