"""
Descarga varios datasets públicos de útiles escolares desde Roboflow Universe (CC BY 4.0),
unifica sus nombres de clase y los fusiona en un único dataset YOLO listo para entrenar.

    python training/merge_datasets.py --roboflow-key TU_API_KEY
    # o guarda la key en ~/.roboflow_key

Resultado: training/datasets/school_supplies/data.yaml
"""

from __future__ import annotations

import argparse
import os
import random
import shutil
from collections import Counter
from pathlib import Path

import yaml

HERE = Path(__file__).resolve().parent
RAW_DIR = HERE / "datasets" / "raw"
OUT_DIR = HERE / "datasets" / "school_supplies"

# Clases finales (en inglés para coincidir con SchoolSupplyCatalog de la app).
CLASSES = [
    "pencil", "pen", "eraser", "ruler", "sharpener", "scissors",
    "glue", "book", "notebook", "compass", "cell phone",
]

# Nombre original (en minúsculas) -> clase final. Lo que no esté aquí se descarta.
ALIASES = {
    "pencil": "pencil", "pensil": "pencil", "pencils - v1 2024-07-18 6:11am": "pencil",
    "pen": "pen", "ballpen": "pen", "ball point": "pen", "ink pen": "pen",
    "eraser": "eraser", "rubber": "eraser", "penghapus": "eraser",
    "ruler": "ruler", "scale": "ruler", "penggaris": "ruler", "rule": "ruler",
    "sharpener": "sharpener", "sharpner": "sharpener", "rautan": "sharpener",
    "scissors": "scissors", "scissor": "scissors",
    "glue": "glue",
    "book": "book", "buku": "book",
    "notebook": "notebook",
    "compass": "compass",
    "phone": "cell phone",
}

# (workspace, proyecto, versión) en Roboflow Universe.
DATASETS = [
    ("national-university-fast", "stationary-items-dataset", 8),
    ("omocomo-naver-com", "online-edu-helper-dataset", 2),
    ("hunaynzm", "stationary-items-detection", 3),
    ("pembelajaranmesin-38zuw", "stationery-qzl8u", 1),
    ("pen-pencil-easer", "pen-pencil-eraser-sgsuv", 1),
    ("stationary-items", "deeplearning_assignment2", 1),
    ("a1-6zprn", "stationery-klnb6", 1),
    ("kimanurak", "stationery-vxciz", 1),
]

SPLITS = {"train": "train", "valid": "val", "test": "test"}


def download(api_key: str) -> list[Path]:
    from roboflow import Roboflow

    rf = Roboflow(api_key=api_key)
    paths = []
    for workspace, project, version in DATASETS:
        target = RAW_DIR / project
        if not (target / "data.yaml").exists():
            print(f"Descargando {workspace}/{project} v{version}...")
            rf.workspace(workspace).project(project).version(version).download("yolov8", location=str(target))
        paths.append(target)
    return paths


def merge(sources: list[Path], val_fraction: float = 0.12, seed: int = 42) -> Path:
    if OUT_DIR.exists():
        shutil.rmtree(OUT_DIR)
    for split in SPLITS.values():
        (OUT_DIR / split / "images").mkdir(parents=True)
        (OUT_DIR / split / "labels").mkdir(parents=True)

    rng = random.Random(seed)
    stats: Counter[str] = Counter()
    images_per_split: Counter[str] = Counter()

    for source in sources:
        names = yaml.safe_load((source / "data.yaml").read_text(encoding="utf-8"))["names"]
        if isinstance(names, dict):
            names = [names[k] for k in sorted(names)]
        remap = {
            i: CLASSES.index(ALIASES[n.strip().lower()])
            for i, n in enumerate(names)
            if n.strip().lower() in ALIASES
        }
        has_valid = (source / "valid" / "images").exists()

        for src_split, dst_split in SPLITS.items():
            img_dir = source / src_split / "images"
            if not img_dir.exists():
                continue
            for img in sorted(img_dir.iterdir()):
                label_file = source / src_split / "labels" / f"{img.stem}.txt"
                lines = []
                if label_file.exists():
                    for row in label_file.read_text().splitlines():
                        parts = row.split()
                        # Solo cajas (5 valores); se ignoran polígonos de segmentación.
                        if len(parts) != 5 or int(parts[0]) not in remap:
                            continue
                        new_cls = remap[int(parts[0])]
                        lines.append(" ".join([str(new_cls), *parts[1:]]))
                        stats[CLASSES[new_cls]] += 1
                if not lines:
                    continue  # imagen sin ninguna clase de interés
                split = dst_split
                if dst_split == "train" and not has_valid and rng.random() < val_fraction:
                    split = "val"
                name = f"{source.name}__{img.name}"
                shutil.copy(img, OUT_DIR / split / "images" / name)
                (OUT_DIR / split / "labels" / f"{Path(name).stem}.txt").write_text("\n".join(lines) + "\n")
                images_per_split[split] += 1

    data_yaml = OUT_DIR / "data.yaml"
    data_yaml.write_text(
        yaml.safe_dump(
            {
                "path": str(OUT_DIR),
                "train": "train/images",
                "val": "val/images",
                "test": "test/images",
                "names": {i: n for i, n in enumerate(CLASSES)},
            },
            sort_keys=False,
            allow_unicode=True,
        ),
        encoding="utf-8",
    )
    print("Imágenes por split:", dict(images_per_split))
    print("Cajas por clase:", dict(stats.most_common()))
    return data_yaml


def main() -> None:
    parser = argparse.ArgumentParser(description=__doc__, formatter_class=argparse.RawDescriptionHelpFormatter)
    parser.add_argument("--roboflow-key", default=os.environ.get("ROBOFLOW_API_KEY"))
    args = parser.parse_args()
    key = args.roboflow_key
    key_file = Path.home() / ".roboflow_key"
    if not key and key_file.exists():
        key = key_file.read_text().strip()
    if not key:
        parser.error("Falta la API key (--roboflow-key, ROBOFLOW_API_KEY o ~/.roboflow_key)")
    print("Dataset listo:", merge(download(key)))


if __name__ == "__main__":
    main()
