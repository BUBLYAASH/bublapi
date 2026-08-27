from http.server import SimpleHTTPRequestHandler, ThreadingHTTPServer
from pathlib import Path


class CustomHandler(SimpleHTTPRequestHandler):

    def send_error(self, code, message=None, explain=None):
        if code == 404:
            error_page = Path("404.html")

            if error_page.exists():
                content = error_page.read_bytes()

                self.send_response(404)
                self.send_header("Content-Type", "text/html; charset=utf-8")
                self.send_header("Content-Length", str(len(content)))
                self.end_headers()

                self.wfile.write(content)
                return

        super().send_error(code, message, explain)


if __name__ == "__main__":
    server = ThreadingHTTPServer(("localhost", 8081), CustomHandler)

    print("Сайт запущен: http://localhost:8081")
    server.serve_forever()