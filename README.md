<div align="center">

# 📐 UtilScan

**Reconoce útiles escolares en tiempo real con la cámara de tu celular, usando IA 100% local.**

Kotlin · Jetpack Compose · CameraX · LiteRT (TensorFlow Lite) · YOLOv8

[![Android CI](https://github.com/AbnerGA7/utilscan/actions/workflows/android.yml/badge.svg)](https://github.com/AbnerGA7/utilscan/actions/workflows/android.yml)
![minSdk](https://img.shields.io/badge/minSdk-24-green)
![License](https://img.shields.io/badge/license-MIT-blue)

</div>

---

## ✨ Qué hace

- 📷 Apunta la cámara a tu escritorio y UtilScan dibuja un recuadro sobre cada útil con su nombre **en español** y su confianza.
- 🧠 **IA on-device**: ninguna imagen sale del teléfono, funciona en modo avión.
- ⚡ Aceleración por **GPU** (con fallback automático a CPU + XNNPACK).
- 🎚️ Ajustes en vivo: confianza mínima, filtro "solo útiles escolares", CPU/GPU y selector de modelo.
- 📊 Resumen agrupado ("✏️ Lápiz ×2", "📏 Regla") + latencia y FPS en pantalla.

## 🧠 El modelo

| | |
|---|---|
| **Arquitectura** | YOLOv8s (Ultralytics) |
| **Pre-entrenamiento** | [Open Images V7](https://storage.googleapis.com/openimages/web/index.html): 601 clases, ~1.7 M imágenes |
| **Formato** | `.tflite` FP16, entrada 640×640 |
| **Útiles que reconoce sin entrenar** | lapicero, borrador, regla, tajador, cartuchera, tijeras, engrapador, calculadora, libro, mochila, cinta adhesiva, sobre, pizarra, laptop, celular, tablet, teclado, mouse, tomatodo, reloj |

¿Por qué este modelo? Los modelos "típicos" de móvil (COCO, EfficientDet-Lite, MobileNet-SSD) **no conocen**
lápices, reglas ni borradores. Open Images V7 sí, así que la app funciona bien desde el primer momento.
Para más precisión (o clases extra como compás, escuadra o plumón) puedes **afinarlo en ~30 min gratis en Colab**:
mira [`training/`](training/README.md).

La app prioriza automáticamente un modelo afinado (`school_supplies.tflite`) si existe.

## 🚀 Cómo correrlo

1. Clona el repo y ábrelo en **Android Studio** (Ladybug o superior).
2. Dale ▶️ Run. La tarea de Gradle `downloadModels` baja el modelo (~22 MB) desde los
   [Releases](https://github.com/AbnerGA7/utilscan/releases) la primera vez.
3. Acepta el permiso de cámara y apunta a tus útiles.

O descarga el APK debug desde la pestaña **Actions → Android CI → Artifacts**.

```bash
./gradlew :app:testDebugUnitTest   # tests unitarios
./gradlew :app:assembleDebug       # APK en app/build/outputs/apk/debug/
```

## 🏗️ Arquitectura

```
app/src/main/java/com/abnerga/utilscan/
├── domain/          # Kotlin puro, sin Android: testeable
│   ├── Detection.kt              BoundingBox (IoU), Detection, FrameResult
│   ├── YoloOutputParser.kt       decodifica la salida [1, 4+C, 8400] de YOLOv8/11
│   ├── NonMaxSuppression.kt      NMS por clase
│   └── SchoolSupplyCatalog.kt    etiquetas del modelo → útiles en español + emoji
├── data/
│   ├── ModelSpec.kt              descubre modelos en assets/models
│   └── YoloDetector.kt           intérprete LiteRT, preprocesado, GPU/CPU
├── camera/
│   ├── CameraPreview.kt          CameraX en Compose (keep-only-latest)
│   └── FrameAnalyzer.kt          ImageProxy → Bitmap rotado
└── ui/
    ├── DetectionViewModel.kt     estado + inferencia en un hilo dedicado
    ├── CameraScreen.kt           cámara + overlay + barra de estado + panel
    ├── DetectionOverlay.kt       dibuja cajas (mapeo FILL_CENTER)
    ├── DetectionPanel.kt         resumen y ajustes
    └── PermissionScreen.kt
```

**Flujo de un frame:** CameraX (RGBA, 4:3) → `FrameAnalyzer` rota → `YoloDetector` redimensiona a 640×640 y
normaliza → LiteRT → `YoloOutputParser` (solo evalúa las clases escolares, así que es rápido aunque el modelo
tenga 601) → NMS → `StateFlow` → Compose dibuja las cajas.

## 🗺️ Ideas para seguir

- [ ] Afinar con fotos propias del aula (ver `training/`).
- [ ] Modo "checklist": marca qué útiles de tu lista ya están en la mochila.
- [ ] Cuantización INT8 para gama baja.
- [ ] Lectura en voz alta de lo detectado (accesibilidad).

## 📄 Licencia

Código bajo [MIT](LICENSE). Los pesos YOLOv8 de Ultralytics se distribuyen bajo AGPL-3.0;
Open Images V7 está bajo CC BY 4.0.
