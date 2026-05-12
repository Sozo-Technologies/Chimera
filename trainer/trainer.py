import os
import csv
import cv2
import mediapipe as mp
from pathlib import Path

from mediapipe.tasks import python
from mediapipe.tasks.python import vision

BASE_DIR = Path(__file__).resolve().parent
RESOURCES_DIR = BASE_DIR / "resources"
OUTPUT_CSV = BASE_DIR / "dataset.csv"


MODEL_PATH = "chimera/src/main/resources/hand_landmarker.task"


def normalize(landmarks: list) -> list[tuple] | None:

    if not landmarks or landmarks[0] is None:
        return None

    ox, oy, oz = landmarks[0]

    normalized = []

    for lm in landmarks:
        if lm is None:
            return None

        x, y, z = lm

        normalized.append((
            x - ox,
            y - oy,
            z - oz
        ))

    return normalized

base_options = python.BaseOptions(
    model_asset_path=MODEL_PATH
)

options = vision.HandLandmarkerOptions(
    base_options=base_options,
    num_hands=1,
    min_hand_detection_confidence=0.5,
    min_hand_presence_confidence=0.5,
    min_tracking_confidence=0.5
)

detector = vision.HandLandmarker.create_from_options(options)

def extract_landmarks(image_path: str) -> list[float] | None:

    image = cv2.imread(image_path)

    if image is None:
        print(f"  [SKIP] Cannot read image: {image_path}")
        return None

    image_rgb = cv2.cvtColor(image, cv2.COLOR_BGR2RGB)

    mp_image = mp.Image(
        image_format=mp.ImageFormat.SRGB,
        data=image_rgb
    )

    result = detector.detect(mp_image)

    if not result.hand_landmarks:
        print(f"  [SKIP] No hand detected: {os.path.basename(image_path)}")
        return None

    hand = result.hand_landmarks[0]

    raw = []

    for lm in hand:
        raw.append((
            lm.x,
            lm.y,
            lm.z
        ))

    if len(raw) != 21:
        print(f"  [SKIP] Incomplete landmarks: {os.path.basename(image_path)}")
        return None

    normalized = normalize(raw)

    if normalized is None:
        print(f"  [SKIP] Normalization failed: {os.path.basename(image_path)}")
        return None

    flat = []

    for x, y, z in normalized:
        flat.extend([x, y, z])

    return flat

def generate(resources_dir: str, output_csv: str):

    labels = sorted([
        d for d in os.listdir(resources_dir)
        if os.path.isdir(os.path.join(resources_dir, d))
    ])

    if not labels:
        print(f"[ERROR] No label folders found in '{resources_dir}'")
        return

    print(f"[INFO] Found labels: {labels}")
    print(f"[INFO] Output: {output_csv}\n")

    total_written = 0
    total_skipped = 0

    with open(output_csv, "w", newline="") as csvfile:

        writer = csv.writer(csvfile)

        header = ["label"]

        for i in range(21):
            header.extend([
                f"x{i}",
                f"y{i}",
                f"z{i}"
            ])

        writer.writerow(header)

        for label in labels:

            label_dir = os.path.join(resources_dir, label)

            images = [
                f for f in os.listdir(label_dir)
                if f.lower().endswith((
                    ".jpg",
                    ".jpeg",
                    ".png",
                    ".bmp"
                ))
            ]

            if not images:
                print(f"[WARN] No images found in: {label_dir}")
                continue

            print(f"[{label.upper()}] Processing {len(images)} images...")

            written = 0
            skipped = 0

            for img_file in images:

                img_path = os.path.join(label_dir, img_file)

                flat = extract_landmarks(img_path)

                if flat is None:
                    skipped += 1
                    total_skipped += 1
                    continue

                writer.writerow([label] + flat)

                written += 1
                total_written += 1

            print(f"  ✔ Written: {written} | Skipped: {skipped}\n")

    print("─" * 50)
    print(f"[DONE] Total written : {total_written}")
    print(f"[DONE] Total skipped : {total_skipped}")
    print(f"[DONE] Saved to      : {output_csv}")

if __name__ == "__main__":

    if not os.path.exists(MODEL_PATH):
        print(f"[ERROR] Missing model: {MODEL_PATH}")
        print("Download:")
        print("https://storage.googleapis.com/mediapipe-models/hand_landmarker/hand_landmarker/float16/1/hand_landmarker.task")
        exit(1)

    generate(RESOURCES_DIR, OUTPUT_CSV)