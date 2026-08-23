import glob
import os
import sys

sys.path.insert(0, os.path.dirname(__file__))
from generate_feature_graphic import create_clean_phone_mockup

SRC_DIR = "assets/screenshots"
OUT_DIR = "assets/screenshots/framed"

def main():
    os.makedirs(OUT_DIR, exist_ok=True)
    for path in sorted(glob.glob(os.path.join(SRC_DIR, "*.png"))):
        name = os.path.basename(path)
        from PIL import Image
        w, h = Image.open(path).size
        framed = create_clean_phone_mockup(path, w, h, corner_radius=110, bezel_width=16)
        framed.save(os.path.join(OUT_DIR, name), "PNG")
        print(f"Framed {name}")

if __name__ == "__main__":
    main()
