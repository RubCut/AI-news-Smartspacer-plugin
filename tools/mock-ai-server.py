#!/usr/bin/env python3
"""A stand-in for a local AI server: the two endpoints this plugin calls.

No Ollama, no model, no API key, no pip install — python3 only. It answers the
model list and returns two ready-made stories, so the whole path from the plugin
to a plain HTTP endpoint and back can be checked on a real phone.

    python3 tools/mock-ai-server.py --port 11434

Loopback, the path that needs no switch — forward the laptop's port to the
phone over USB, then use http://localhost:11434/v1 as the base URL:

    adb reverse tcp:11434 tcp:11434

LAN, the path that needs "Allow unencrypted connections" switched on, because
Android refuses cleartext to a host outside the device:

    http://<laptop address>:11434/v1

Both cases are visible in the terminal here: every request line, and whether an
Authorization header arrived.
"""

import argparse
import json
import socket
from http.server import BaseHTTPRequestHandler, ThreadingHTTPServer

STORIES = [
    {
        "short": "Mock server reached",
        "title": "The plugin talked to a plain HTTP server, so the local path works",
        "body": (
            "## Framing\n\nIf you are reading this on the smartspace, the request "
            "made it through with the base URL you typed, the response arrived "
            "whole, and the parser accepted it.\n\n"
            "## What the plugin sent\n\n"
        ),
    },
    {
        "short": "Round two, with a headline",
        "title": "Second story proves more than one item is kept",
        "body": "## Why two\n\nA single story could come from a cache. Two means "
                "this reply was parsed, stored and handed to the target.\n",
    },
]


class Handler(BaseHTTPRequestHandler):
    protocol_version = "HTTP/1.1"

    def do_GET(self):
        if not self.path.rstrip("/").endswith("/models"):
            return self.reply(404, {"error": {"message": "no such path: %s" % self.path}})
        self.report("listing models")
        self.reply(
            200,
            {
                "object": "list",
                "data": [{"id": name, "object": "model", "owned_by": "mock"} for name in MODELS],
            },
        )

    def do_POST(self):
        if not self.path.rstrip("/").endswith("/chat/completions"):
            return self.reply(404, {"error": {"message": "no such path: %s" % self.path}})
        try:
            payload = json.loads(self.rfile.read(int(self.headers.get("Content-Length", 0))))
        except (ValueError, UnicodeDecodeError) as error:
            return self.reply(400, {"error": {"message": "unreadable body: %s" % error}})

        prompt = ""
        for message in payload.get("messages", []):
            if message.get("role") == "user":
                prompt = str(message.get("content", ""))
        self.report(
            "asked for model=%s, %d chars of prompt, %s"
            % (payload.get("model"), len(prompt), key_note(self.headers))
        )

        excerpt = " ".join(prompt.split())[:220] or "no prompt"
        stories = json.dumps(
            [dict(story, body=story["body"] + "`%s…`" % excerpt) for story in STORIES]
        )
        self.reply(
            200,
            {
                "id": "chatcmpl-mock",
                "object": "chat.completion",
                "model": payload.get("model"),
                "choices": [
                    {
                        "index": 0,
                        "message": {"role": "assistant", "content": stories},
                        "finish_reason": "stop",
                    }
                ],
                "usage": {"total_tokens": 0},
            },
        )

    def reply(self, status, body):
        raw = json.dumps(body).encode("utf-8")
        self.send_response(status)
        self.send_header("Content-Type", "application/json")
        self.send_header("Content-Length", str(len(raw)))
        self.end_headers()
        self.wfile.write(raw)

    def report(self, what):
        print("%s %s → %s" % (self.client_address[0], self.command, what), flush=True)

    def log_message(self, *args):
        pass  # the one line per request above is the log we want.


MODELS = ["mock-mini", "mock-pro", "mock-reasoner"]


def key_note(headers):
    return "with an API key" if headers.get("Authorization") else "with no API key"


def main():
    parser = argparse.ArgumentParser(description=__doc__.splitlines()[0])
    parser.add_argument("--port", type=int, default=11434, help="default: 11434, the Ollama port")
    parser.add_argument(
        "--host",
        default="0.0.0.0",
        help="0.0.0.0 answers the phone over the LAN; 127.0.0.1 only this machine",
    )
    args = parser.parse_args()

    server = ThreadingHTTPServer((args.host, args.port), Handler)
    print("mock AI server on http://%s:%d/v1" % (args.host, args.port))
    print("base URL for the plugin:  http://localhost:%d/v1  (after adb reverse)" % args.port)
    print("                          http://%s:%d/v1  (+ allow unencrypted connections)"
          % (lan_address(), args.port))
    print("Ctrl-C to stop.")
    try:
        server.serve_forever()
    except KeyboardInterrupt:
        print("\nstopped.")


def lan_address():
    """The address a phone on the same network would have to use."""
    probe = socket.socket(socket.AF_INET, socket.SOCK_DGRAM)
    try:
        probe.connect(("8.8.8.8", 80))
        return probe.getsockname()[0]
    except OSError:
        return "<this machine's address>"
    finally:
        probe.close()


if __name__ == "__main__":
    main()
