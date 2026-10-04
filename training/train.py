"""
Entrena (fine-tuning) un detector YOLO de útiles escolares y lo exporta a .tflite para UtilScan.

Uso típico (Google Colab con GPU o una PC con CUDA):

    pip install "ultralytics>=8.3,<8.4" roboflow
    python training/train.py --roboflow-key TU_API_KEY \
        --workspace hunaynzm --project stationary-items-detection --version 1

O con un dataset local en formato YOLO (data.yaml):

    python training/train.py --data ruta/a/data.yaml

Al terminar copia automáticamente el modelo y sus etiquetas a
app/src/main/assets/models/school_supplies.tflite (+ _labels.txt); la app lo usa con prioridad.
"""

from __future__ import annotations

import argparse
import shutil
from pathlib import Path

REPO_ROOT = Path(__file__).resolve().parents[1]
ASSETS_DIR = REPO_ROOT / "app" / "src" / "main" / "assets" / "models"


def download_roboflow(api_key: str, workspace: str, project: str, version: int) -> Path:
    from roboflow import Roboflow

    rf = Roboflow(api_key=api_key)
    dataset = rf.workspace(workspace).project(project).version(version).download(
        "yolov8", location=str(Path(__file__).parent / "datasets" / project)
    )
    return Path(dataset.location) / "data.yaml"


def main() -> None:
    parser = argparse.ArgumentParser(description=__doc__, formatter_class=argparse.RawDescriptionHelpFormatter)
    parser.add_argument("--data", type=Path, help="data.yaml de un dataset en formato YOLO")
    parser.add_argument("--roboflow-key", help="API key de Roboflow (https://app.roboflow.com/settings/api)")
    parser.add_argument("--workspace", default="hunaynzm")
    parser.add_argument("--project", default="stationary-items-detection")
    parser.add_argument("--version", type=int, default=1)
    parser.add_argument(
        "--weights",
        default="yolov8s-oiv7.pt",
        help="Pesos iniciales. yolov8s-oiv7.pt ya conoce Pen/Ruler/Eraser/Scissors..., ideal para fine-tuning",
    )
    parser.add_argument("--epochs", type=int, default=100)
    parser.add_argument("--imgsz", type=int, default=640)
    parser.add_argument("--batch", type=int, default=16)
    parser.add_argument("--no-copy", action="store_true", help="No copiar el modelo a los assets de la app")
    args = parser.parse_args()

    if args.data is None:
        if not args.roboflow_key:
            parser.error("Indica --data o --roboflow-key")
        args.data = download_roboflow(args.roboflow_key, args.workspace, args.project, args.version)

    from ultralytics import YOLO

    model = YOLO(args.weights)
    model.train(
        data=str(args.data),
        epochs=args.epochs,
        imgsz=args.imgsz,
        batch=args.batch,
        patience=25,
        # Aumentaciones útiles para objetos pequeños y alargados (lápices, reglas):
        degrees=15,
        flipud=0.2,
        mosaic=1.0,
        mixup=0.1,
        project=str(Path(__file__).parent / "runs"),
        name="school_supplies",
        exist_ok=True,
    )

    metrics = model.val()
    print(f"mAP50: {metrics.box.map50:.3f}  mAP50-95: {metrics.box.map:.3f}")

    best = YOLO(Path(model.trainer.save_dir) / "weights" / "best.pt")
    exported = Path(best.export(format="tflite", imgsz=args.imgsz, half=True))
    tflite = next(exported.parent.glob("*_float16.tflite"), exported) if exported.is_file() else next(
        exported.glob("*_float16.tflite")
    )
    print("Modelo exportado:", tflite)

    if not args.no_copy:
        ASSETS_DIR.mkdir(parents=True, exist_ok=True)
        shutil.copy(tflite, ASSETS_DIR / "school_supplies.tflite")
        labels = [best.names[i] for i in range(len(best.names))]
        (ASSETS_DIR / "school_supplies_labels.txt").write_text("\n".join(labels) + "\n", encoding="utf-8")
        print(f"Copiado a {ASSETS_DIR}. Clases: {labels}")


if __name__ == "__main__":
    main()
