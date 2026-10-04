# 🧠 Entrenamiento del modelo

UtilScan funciona **sin entrenar nada**: trae YOLOv8s pre-entrenado en
[Open Images V7](https://storage.googleapis.com/openimages/web/index.html) (601 clases), que ya reconoce
lapicero (*Pen*), borrador (*Eraser*), regla (*Ruler*), tajador (*Pencil sharpener*), cartuchera (*Pencil case*),
tijeras, engrapador, calculadora, libro, mochila, cinta adhesiva, laptop, celular y más.

Si quieres **más precisión** o clases que Open Images no tiene (lápiz vs. lapicero, compás, escuadra, plumón,
goma en barra…), afina el modelo con un dataset de útiles escolares.

## Opción A: Google Colab (recomendada, GPU gratis)

1. Abre [`UtilScan_entrenamiento.ipynb`](UtilScan_entrenamiento.ipynb) en Colab
   (`Archivo → Abrir notebook → GitHub → AbnerGA7/utilscan`).
2. Elige GPU T4, pega tu API key de Roboflow y ejecuta todo.
3. Descarga `school_supplies.tflite` y `school_supplies_labels.txt` y cópialos a
   `app/src/main/assets/models/`.

## Opción B: tu PC (con GPU NVIDIA)

```bash
pip install -r training/requirements.txt
python training/train.py --roboflow-key TU_API_KEY \
    --workspace hunaynzm --project stationary-items-detection --version 1
```

El script entrena, valida, exporta a TFLite FP16 y copia el modelo a los assets de la app.

## Datasets recomendados (Roboflow Universe)

| Dataset | Imágenes | Clases |
|---|---|---|
| [Stationary Items Detection](https://universe.roboflow.com/hunaynzm/stationary-items-detection) | 99 | libro, tijeras, lápiz, lapicero, compás, borrador, escuadra, regla, tajador… |
| [Stationary Item Detector](https://universe.roboflow.com/search?q=class%3Apencil+and+ruler) | ~500 | plumón, lapicero, lápiz, tijeras, engrapador, cinta, borrador, goma, regla, tajador |
| [Stationery (PembelajaranMesin)](https://universe.roboflow.com/pembelajaranmesin-38zuw/stationery-qzl8u) | 100 | libro, regla, borrador, lápiz, tajador |
| [Pen Pencil Eraser](https://universe.roboflow.com/pen-pencil-easer/pen-pencil-eraser-sgsuv) | 151 | lapicero, lápiz, borrador |

> 💡 **Tip para mejor precisión:** combina 2-3 datasets en Roboflow (*Merge*), y agrega 50-100 fotos
> **tuyas** tomadas con el celular, en tu escritorio y con tu iluminación. Eso mejora más que cualquier otro cambio.

## ¿Por qué partir de `yolov8s-oiv7.pt`?

Esos pesos ya aprendieron qué es un *Pen*, un *Ruler* o un *Eraser* con millones de imágenes de Open Images.
Al afinarlos, el modelo converge más rápido y generaliza mejor que partiendo de COCO (que no tiene útiles).

## Exportar otro modelo pre-entrenado

El workflow [`export-model.yml`](../.github/workflows/export-model.yml) exporta cualquier peso de Ultralytics
(por ejemplo `yolov8m-oiv7.pt`, más preciso pero más lento) y lo publica como release:
`Actions → Exportar modelo YOLO a LiteRT → Run workflow`.
