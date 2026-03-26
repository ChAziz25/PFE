import subprocess
from pathlib import Path

def ReadSettings(lang):
    output = []
    
    folder = Path("/root/sandbox")
    files = [f.stem for f in folder.glob("*.py")]
    
    for file in files:
        if lang == file :

            with open(f"/root/sandbox/{file}.py", "r") as ReadFile:
                lines = ReadFile.readlines()
                for line in lines:

                    if line.startswith("import"):
                        output.append(line)
                    elif line.startswith("from"):
                        output.append(line)

                    if ("arg" in line and "=" in line):
                        output.append(line)
    return output

def SaveSettings(lang, content):
    folder = Path("/root/sandbox")
    files = [f.stem for f in folder.glob("*.py")]
    for file in files:
        if lang == file :
            with open(f"/root/sandbox/{file}.py", "w") as WriteFile:
                WriteFile.writelines(content)