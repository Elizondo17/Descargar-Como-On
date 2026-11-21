import json
import os
import sys
import time
from http.server import SimpleHTTPRequestHandler, ThreadingHTTPServer
import urllib.parse

DATA_DIR = os.path.abspath("cloud_data")
WEB_DIR = os.path.abspath("web_download")
os.makedirs(DATA_DIR, exist_ok=True)
os.makedirs(WEB_DIR, exist_ok=True)

USERS_FILE = os.path.join(DATA_DIR, "users.json")
ALERTS_FILE = os.path.join(DATA_DIR, "alerts.json")
KNOWLEDGE_FILE = os.path.join(DATA_DIR, "sales_knowledge.json")
ROLE_MATRIX_FILE = os.path.join(DATA_DIR, "role_matrix.json")

def load_json(path, default):
    if not os.path.exists(path):
        return default
    try:
        with open(path, "r", encoding="utf-8") as f:
            return json.load(f)
    except Exception:
        return default

def save_json(path, data):
    tmp = path + ".tmp"
    with open(tmp, "w", encoding="utf-8") as f:
        json.dump(data, f, ensure_ascii=False, indent=2)
    os.replace(tmp, path)

class SyncAndStaticRequestHandler(SimpleHTTPRequestHandler):
    def __init__(self, *args, **kwargs):
        super().__init__(*args, directory=WEB_DIR, **kwargs)

    def end_headers(self):
        self.send_header("Access-Control-Allow-Origin", "*")
        self.send_header("Access-Control-Allow-Methods", "GET, POST, OPTIONS, PUT, DELETE")
        self.send_header("Access-Control-Allow-Headers", "Content-Type, Authorization")
        super().end_headers()

    def do_OPTIONS(self):
        self.send_response(200)
        self.end_headers()

    def do_GET(self):
        parsed = urllib.parse.urlparse(self.path)
        path = parsed.path

        if path.startswith("/api/sync/"):
            self.handle_sync_get(path)
            return

        super().do_GET()

    def do_POST(self):
        parsed = urllib.parse.urlparse(self.path)
        path = parsed.path

        if path.startswith("/api/sync/"):
            content_length = int(self.headers.get("Content-Length", 0))
            body_bytes = self.rfile.read(content_length) if content_length > 0 else b"{}"
            try:
                body_json = json.loads(body_bytes.decode("utf-8"))
            except Exception:
                body_json = {}

            self.handle_sync_post(path, body_json)
            return

        self.send_response(404)
        self.end_headers()

    def send_json(self, data, status=200):
        response_bytes = json.dumps(data, ensure_ascii=False).encode("utf-8")
        self.send_response(status)
        self.send_header("Content-Type", "application/json; charset=utf-8")
        self.send_header("Content-Length", str(len(response_bytes)))
        self.end_headers()
        self.wfile.write(response_bytes)

    def handle_sync_get(self, path):
        if path == "/api/sync/status":
            self.send_json({"status": "ok", "timestamp": int(time.time() * 1000)})
            return

        if path == "/api/sync/users" or path == "/api/sync/users/":
            users = load_json(USERS_FILE, {})
            self.send_json(list(users.values()))
            return

        if path.startswith("/api/sync/users/"):
            username = path.replace("/api/sync/users/", "").strip().lower()
            users = load_json(USERS_FILE, {})
            user = users.get(username)
            if user:
                self.send_json(user)
            else:
                self.send_json({"error": "User not found"}, status=404)
            return

        if path == "/api/sync/alerts" or path == "/api/sync/alerts/":
            alerts = load_json(ALERTS_FILE, [])
            self.send_json(alerts[-20:])
            return

        if path == "/api/sync/sales_knowledge" or path == "/api/sync/sales_knowledge/":
            knowledge = load_json(KNOWLEDGE_FILE, [])
            self.send_json(knowledge)
            return

        if path == "/api/sync/role_matrix" or path == "/api/sync/role_matrix/":
            default_matrix = {
                "opPsaEnabled": True,
                "opScriptsEnabled": False,
                "opAutomationEnabled": False,
                "opDashboardEnabled": False,
                "opAiSalesEnabled": False,
                "opPricingEditEnabled": False,
                "suPsaEnabled": True,
                "suScriptsEnabled": True,
                "suAutomationEnabled": True,
                "suDashboardEnabled": True,
                "suSuperuserEnabled": True
            }
            matrix = load_json(ROLE_MATRIX_FILE, default_matrix)
            self.send_json(matrix)
            return

        self.send_json({"error": "Endpoint not found"}, status=404)

    def handle_sync_post(self, path, body):
        if path == "/api/sync/role_matrix":
            save_json(ROLE_MATRIX_FILE, body)
            self.send_json({"status": "ok", "matrix": body})
            return

        if path == "/api/sync/users/delete":
            username = body.get("username", "").strip().lower()
            users = load_json(USERS_FILE, {})
            if username in users:
                del users[username]
                save_json(USERS_FILE, users)
            self.send_json({"status": "ok", "deleted": username})
            return
        if path == "/api/sync/users/update" or path == "/api/sync/users":
            username = body.get("username", "").strip().lower()
            if not username:
                self.send_json({"error": "Username required"}, status=400)
                return

            users = load_json(USERS_FILE, {})
            current = users.get(username, {})
            current.update(body)
            current["username"] = username
            current["updatedAt"] = int(time.time() * 1000)
            users[username] = current
            save_json(USERS_FILE, users)
            self.send_json({"status": "ok", "user": current})
            return

        if path == "/api/sync/alerts":
            title = body.get("title", "").strip()
            body_text = body.get("body", "").strip()
            if not title:
                self.send_json({"error": "Title required"}, status=400)
                return

            alert_item = {
                "id": body.get("id") or f"alert-{int(time.time()*1000)}",
                "title": title,
                "body": body_text,
                "severity": body.get("severity", "CRITICAL"),
                "requirePause": bool(body.get("requirePause", False)),
                "timestamp": int(time.time() * 1000),
                "sender": body.get("sender", "Superusuario")
            }
            alerts = load_json(ALERTS_FILE, [])
            alerts.append(alert_item)
            if len(alerts) > 100:
                alerts = alerts[-100:]
            save_json(ALERTS_FILE, alerts)
            self.send_json({"status": "ok", "alert": alert_item})
            return

        if path == "/api/sync/sales_knowledge" or path == "/api/sync/sales_knowledge/rate":
            knowledge_list = load_json(KNOWLEDGE_FILE, [])

            incoming_items = body.get("items") if isinstance(body.get("items"), list) else [body]
            for in_item in incoming_items:
                if not isinstance(in_item, dict):
                    continue
                k_id = in_item.get("id")
                found = False
                for k in knowledge_list:
                    if k.get("id") == k_id and k_id:
                        k["successScore"] = k.get("successScore", 1) + in_item.get("increment", 1)
                        k["usageCount"] = k.get("usageCount", 1) + 1
                        found = True
                        break

                if not found and in_item.get("winningContent"):
                    new_item = {
                        "id": k_id or f"know-{int(time.time()*1000)}",
                        "category": in_item.get("category", "GENERAL_WINNING"),
                        "triggerContext": in_item.get("triggerContext", ""),
                        "winningContent": in_item.get("winningContent"),
                        "successScore": in_item.get("successScore", 1),
                        "usageCount": in_item.get("usageCount", 1),
                        "isQuestion": bool(in_item.get("isQuestion", False)),
                        "createdAt": int(time.time() * 1000)
                    }
                    knowledge_list.append(new_item)

            save_json(KNOWLEDGE_FILE, knowledge_list)
            self.send_json({"status": "ok", "count": len(knowledge_list)})
            return

        self.send_json({"error": "Action not supported"}, status=404)

def run_server(port=3000):
    server_address = ("", port)
    httpd = ThreadingHTTPServer(server_address, SyncAndStaticRequestHandler)
    print(f"Sync & Static server running on port {port}")
    try:
        httpd.serve_forever()
    except Exception as e:
        print(f"Server exception: {e}")

if __name__ == "__main__":
    port = int(sys.argv[1]) if len(sys.argv) > 1 else 3000
    run_server(port)
