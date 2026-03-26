import subprocess

python_arg = ["python", "-c"]

def PythonSandbox(code):
    process = subprocess.run(
            python_arg + [code],
            capture_output=True,
            text=True
            )
    return process