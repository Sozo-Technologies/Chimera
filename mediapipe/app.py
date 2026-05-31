import asyncio
import importlib
import subprocess
import sys
import json

REQUIRED_LIBRARIES = {
    "websockets": "websockets",
    "mediapipe": "mediapipe",
    "numpy": "numpy",
    "cv2": "opencv-python"
}

def ensure_package(module_name, package_name):
    try:
        importlib.import_module(module_name)
        print(f"[PS]: {package_name} already installed")
    except ImportError:
        print(f"[PS]: Installing {package_name}...")
        subprocess.check_call([sys.executable, "-m", "pip", "install", package_name])
        print(f"[PS]: Installed {package_name}")

for module, package in REQUIRED_LIBRARIES.items():
    ensure_package(module, package)

import websockets
import mediapipe as mp
import numpy as np
import cv2
from pathlib import Path

from mediapipe.tasks import python
from mediapipe.tasks.python import vision

BaseOptions           = python.BaseOptions
HandLandmarker        = vision.HandLandmarker
HandLandmarkerOptions = vision.HandLandmarkerOptions
VisionRunningMode     = vision.RunningMode

MODEL_PATH = Path(sys.argv[1]).resolve()
print(f"[PS]: MODEL PATH: {MODEL_PATH}", flush=True)

options = HandLandmarkerOptions(
    base_options=BaseOptions(model_asset_path=str(MODEL_PATH)),
    num_hands=2,
    running_mode=VisionRunningMode.VIDEO
)

detector = HandLandmarker.create_from_options(options)
frame_id = 0

EMPTY_RESPONSE = json.dumps({
    "landmarks":  [{} for _ in range(21)],
    "world":      [{} for _ in range(21)],
    "visibility": [0.0] * 21
}, separators=(",", ":"))


def validate_frame(message):
    if not isinstance(message, (bytes, bytearray)):
        return None

    if len(message) < 12:
        print("[PS]: Invalid packet size")
        return None

    width    = int.from_bytes(message[0:4],  "big")
    height   = int.from_bytes(message[4:8],  "big")
    channels = int.from_bytes(message[8:12], "big")
    image_bytes = message[12:]

    expected = width * height * channels

    if len(image_bytes) != expected:
        print(f"[PS]: Frame mismatch (expected={expected}, got={len(image_bytes)})")
        return None

    return width, height, channels, image_bytes


def safe_visibility(lm):
    for attr in ("presence", "visibility", "score"):
        val = getattr(lm, attr, None)
        if val is not None:
            return round(float(val), 4)
    return 1.0


def extract_left_hand(frame):
    global frame_id

    rgb      = cv2.cvtColor(frame, cv2.COLOR_BGR2RGB)
    mp_image = mp.Image(image_format=mp.ImageFormat.SRGB, data=rgb)

    frame_id += 1
    result = detector.detect_for_video(mp_image, frame_id * 33)

    if not result.hand_landmarks:
        return None

    for i, handedness_list in enumerate(result.handedness):
        if handedness_list[0].category_name != "Left":
            continue

        img_lm   = result.hand_landmarks[i]
        world_lm = result.hand_world_landmarks[i]

        landmarks  = []
        world      = []
        visibility = []

        for lm, wlm in zip(img_lm, world_lm):
            landmarks.append({
                "x": round(lm.x, 6),
                "y": round(lm.y, 6),
                "z": round(lm.z, 6)
            })
            world.append({
                "x": round(wlm.x, 6),
                "y": round(wlm.y, 6),
                "z": round(wlm.z, 6)
            })
            visibility.append(safe_visibility(lm))

        return {
            "landmarks":  landmarks,
            "world":      world,
            "visibility": visibility
        }

    return None


async def handler(websocket):
    print("[PS]: Client Connected")

    while True:
        try:
            message   = await websocket.recv()
            validated = validate_frame(message)

            if validated is None:
                await websocket.send(EMPTY_RESPONSE)
                continue

            width, height, channels, image_bytes = validated

            frame = np.frombuffer(
                image_bytes, dtype=np.uint8
            ).reshape((height, width, channels))

            hand = extract_left_hand(frame)

            if hand is None:
                await websocket.send(EMPTY_RESPONSE)
            else:
                await websocket.send(json.dumps(hand, separators=(",", ":")))

        except websockets.ConnectionClosed:
            print("[PS]: Client Disconnected")
            break

        except Exception as e:
            print(f"[PS]: ERROR: {e}", flush=True)
            await websocket.send(EMPTY_RESPONSE)


async def main():
    print("[PS]: Starting MediaPipe Server (Left Hand Only)...")

    async with websockets.serve(
        handler,
        "localhost",
        8765,
        max_size=None,
        ping_interval=None
    ):
        print("[PS]: MediaPipe Server Running")
        print("[PS]: ws://localhost:8765")
        print("READY", flush=True)
        await asyncio.Future()


if __name__ == "__main__":
    try:
        asyncio.run(main())
    except KeyboardInterrupt:
        print("\n[PS]: Server Stopped")