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
echo TU_API_KEY > ~/.roboflow_key            # o exporta ROBOFLOW_API_KEY
python training/merge_datasets.py            # descarga y fusiona los datasets
python training/train.py --data training/datasets/school_supplies/data.yaml
```

El script entrena YOLO11s, valida, exporta a TFLite FP16 y copia el modelo a los assets de la app.

## Dataset fusionado

`merge_datasets.py` combina estos datasets de [Roboflow Universe](https://universe.roboflow.com) (todos CC BY 4.0)
y unifica sus etiquetas en 11 clases: `pencil, pen, eraser, ruler, sharpener, scissors, glue, book, notebook, compass, cell phone`.

| Dataset | Imágenes | Aporta |
|---|---|---|
| [Stationary Items Dataset](https://universe.roboflow.com/national-university-fast/stationary-items-dataset) | 1162 | lápiz, lapicero, borrador, regla, tajador, libro, cuaderno |
| [Online Edu Helper](https://universe.roboflow.com/omocomo-naver-com/online-edu-helper-dataset) | 3489 | lapicero, regla, tijeras, goma, celular, libro |
| [Stationary Items Detection](https://universe.roboflow.com/hunaynzm/stationary-items-detection) | 99 | compás, tijeras, tajador, lápiz… |
| [Stationery (PembelajaranMesin)](https://universe.roboflow.com/pembelajaranmesin-38zuw/stationery-qzl8u) | 100 | regla, borrador, lápiz, tajador, libro |
| [Pen Pencil Eraser](https://universe.roboflow.com/pen-pencil-easer/pen-pencil-eraser-sgsuv) | 63 | lapicero, lápiz, borrador |
| [DeepLearning_Assignment2](https://universe.roboflow.com/stationary-items/deeplearning_assignment2) | 41 | tajador, regla, lápiz, borrador |
| [Stationery (A1)](https://universe.roboflow.com/a1-6zprn/stationery-klnb6) | 50 | cuaderno, lapicero, lápiz, borrador, regla |
| [Stationery (Kimanurak)](https://universe.roboflow.com/kimanurak/stationery-vxciz) | 144 | lapicero, lápiz, borrador |

Resultado: **3966 imágenes** (3187 train / 491 val / 288 test) y ~5.800 cajas.

> 💡 **Tip para mejor precisión:** combina 2-3 datasets en Roboflow (*Merge*), y agrega 50-100 fotos
> **tuyas** tomadas con el celular, en tu escritorio y con tu iluminación. Eso mejora más que cualquier otro cambio.

## Exportar otro modelo pre-entrenado

El workflow [`export-model.yml`](../.github/workflows/export-model.yml) exporta cualquier peso de Ultralytics
(por ejemplo `yolov8m-oiv7.pt`, más preciso pero más lento) y lo publica como release:
`Actions → Exportar modelo YOLO a LiteRT → Run workflow`.
