from http.server import BaseHTTPRequestHandler, HTTPServer
import subprocess
import json

HOST = "0.0.0.0"
PORT = 8080

class Handler(BaseHTTPRequestHandler):
    def do_POST(self):
        content_length = int(self.headers.get('Content-Length', 0))
        body = self.rfile.read(content_length)

        data = json.loads(body)
        command = data.get("command", "")

        try:
            result = subprocess.run(
                command,
                shell=True,
                capture_output=True,
                text=True
            )
            output = result.stdout
        except Exception as e:
            output = str(e)

        self.send_response(200)
        self.send_header("Content-Type", "application/json")
        self.end_headers()

        response = {"output": output}
        self.wfile.write(json.dumps(response).encode())

    def log_message(self, format, *args):
        return  # disable default logs

def main():
    print(f"HTTP server running on {HOST}:{PORT}", flush=True)
    print("READY", flush=True)
    HTTPServer((HOST, PORT), Handler).serve_forever()

if __name__ == '__main__':
    main()
