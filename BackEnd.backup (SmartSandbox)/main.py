import modal
from fastapi import FastAPI
from fastapi.middleware.cors import CORSMiddleware

# Install fastapi and uvicorn
image = (
    modal.Image.debian_slim()
    .pip_install(["fastapi", "uvicorn"])
    .add_local_file("settings.py", "/root/app_settings.py")
    .add_local_dir("sandbox", "/root/sandbox")
)

# Create a Modal app & FastAPI app
app = modal.App("code-sandbox")
web_app = FastAPI()

# Add CORS middleware
web_app.add_middleware(
    CORSMiddleware,
    allow_origins=["*"],  # allow all origins
    allow_methods=["*"],
    allow_headers=["*"],
)

# Define the routes
@web_app.post("/settings")
async def settings(data: dict):
    lang = data["lang"]
    try:
        match lang:
            case "python":
                from app_settings import ReadSettings as settings
                return settings(lang)
            case _:
                return "Language not supported"
    except Exception as e:
        return str(e)

@web_app.get("/languages")
async def sandboxs():
    from pathlib import Path
    folder = Path("/root/sandbox")
    files = [f.stem for f in folder.glob("*.py")]
    return files

@app.function(timeout=5, image=image)
def execute(code: str, lang: str):
    try:
        match lang:
            case "python":
                import sandbox.python as python
                result = python.PythonSandbox(code)
                return result.stdout or result.stderr
            case _:
                return "Language not supported"
    except Exception as e:
        return str(e)

# Configure the routes
@web_app.post("/run")
async def run_code(data: dict):
    code = data["text"]
    lang = data["lang"]
    result = await execute.remote.aio(code, lang)
    return {"result": result}

# Run the app
@app.function(image=image)
@modal.asgi_app()
def fastapi_app():
    return web_app