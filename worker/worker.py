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

workDir = "/"

def execute(container_ID, command):
    global workDir
    try:
        if command.startswith("cd "):
            new_dir = command[3:].strip()
            test = subprocess.run(
                ["docker", "exec", container_ID, "sh", "-c", f"cd {workDir} && cd {new_dir} && pwd"],
                capture_output=True, text=True, timeout=30
            )
            if test.returncode == 0:
                workDir = test.stdout.strip()
            return test.stderr.strip() if test.returncode != 0 else ""

        result = subprocess.run(
            ["docker", "exec", container_ID, "sh", "-c", f"cd {workDir} && {command}"],
            capture_output=True, text=True, timeout=30
        )
        return (result.stdout + result.stderr).strip()

    except Exception as e:
        return f"Error: {str(e)}"

def checkContainer(container_ID):
    try:
        result = subprocess.run(
            ["docker", "inspect", "-f", "{{.State.Running}}", container_ID],
            capture_output=True,
            text=True
        )
        return result.stdout.strip() == "true"
    except Exception as e:
        print(f"Error checking container {container_ID}: {str(e)}")
        return False

def run(container_ID):
    try:
        result = subprocess.run(
            ["docker", "start", container_ID],
            capture_output=True,
            text=True
        )
        print(f"Started container {container_ID}: {result.stdout.strip()}")
    except Exception as e:
        print(f"Error starting container {container_ID}: {str(e)}")
def addTool(container_ID, script, script_type):
    try:
        if script_type == "python":
            ext = "py"
        elif script_type == "bash":
            ext = "sh"
        else:
            print(f"Unsupported script type: {script_type}")
            return

        filename = f"tool_{container_ID}.{ext}"

        # write the script into the container
        result = execute(container_ID, f"mkdir -p /tools && echo '{script}' > /tools/{filename}")
        print(f"Tool added to container {container_ID}: {result}")

    except Exception as e:
        print(f"Error adding tool to container {container_ID}: {str(e)}")

def stopContainer(container_ID):
    try:
        result = subprocess.run(
            ["docker", "stop", container_ID],
            capture_output=True,
            text=True
        )
        print(f"Stopped container {container_ID}: {result.stdout.strip()}")
    except Exception as e:
        print(f"Error stopping container {container_ID}: {str(e)}")

print("Worker started")

for message in consumer:
    data = message.value or {}
    print("RAW MESSAGE:", data)

    msg_type = data.get("type")

    if msg_type == "EXECUTE_COMMAND":
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
    elif msg_type == "NEW_TOOL":
        script = data.get("script")
        targetContainerIds = data.get("targetContainerIds")
        script_type = data.get("script_type")

        for container_ID in targetContainerIds:
            print(f"> adding tool container {container_ID}")
            running = checkContainer(container_ID)
            if running:
                addTool(container_ID, script, script_type)
            else:
                run(container_ID)
                addTool(container_ID, script, script_type)
                stopContainer(container_ID)

        response = {
            "type": msg_type,
            "script": script,
            "targetContainerIds": targetContainerIds,
            "script_type": script_type
        }
    else:
        response = {
            "type": msg_type,
            "commandId": command_ID,
            "error": "Invalid message type"
        }

    print("RESPONSE:", response)

    future = producer.send('results', response)