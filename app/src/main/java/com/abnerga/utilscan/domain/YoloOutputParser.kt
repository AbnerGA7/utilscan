package com.abnerga.utilscan.domain

/**
 * Decodifica la salida cruda de un detector YOLOv8/YOLO11 exportado con Ultralytics.
 *
 * La salida tiene forma `[1, 4 + numClasses, numAnchors]` (canales primero, el formato por
 * defecto) o `[1, numAnchors, 4 + numClasses]`. Cada anchor contiene `cx, cy, w, h` seguido
 * de un score por clase (ya pasado por sigmoide, sin "objectness").
 */
class YoloOutputParser(
    private val labels: List<String>,
    /** Índices de clase a considerar. `null` = todas. */
    private val allowedClasses: Set<Int>? = null,
) {
    private val numClasses = labels.size
    private val channels = 4 + numClasses
    private val candidateClasses: IntArray =
        allowedClasses?.filter { it in 0 until numClasses }?.sorted()?.toIntArray()
            ?: IntArray(numClasses) { it }

    /**
     * @param output tensor de salida aplanado.
     * @param shape forma del tensor, p. ej. `[1, 605, 8400]`.
     * @param inputSize lado de la imagen de entrada del modelo (para coordenadas en píxeles).
     */
    fun parse(
        output: FloatArray,
        shape: IntArray,
        inputSize: Int,
        scoreThreshold: Float,
        iouThreshold: Float = 0.45f,
        maxDetections: Int = 20,
    ): List<Detection> {
        require(shape.size == 3) { "Se esperaba un tensor de 3 dimensiones, llegó ${shape.contentToString()}" }
        val channelsFirst = when (channels) {
            shape[1] -> true
            shape[2] -> false
            else -> error(
                "La salida ${shape.contentToString()} no coincide con ${labels.size} etiquetas " +
                    "(se esperaban $channels canales). ¿labels.txt corresponde al modelo?"
            )
        }
        val numAnchors = if (channelsFirst) shape[2] else shape[1]
        fun value(anchor: Int, channel: Int): Float =
            if (channelsFirst) output[channel * numAnchors + anchor] else output[anchor * channels + channel]

        val candidates = ArrayList<Detection>()
        for (anchor in 0 until numAnchors) {
            var bestClass = -1
            var bestScore = scoreThreshold
            for (cls in candidateClasses) {
                val score = value(anchor, 4 + cls)
                if (score > bestScore) {
                    bestScore = score
                    bestClass = cls
                }
            }
            if (bestClass < 0) continue

            var cx = value(anchor, 0)
            var cy = value(anchor, 1)
            var w = value(anchor, 2)
            var h = value(anchor, 3)
            // Algunas exportaciones devuelven píxeles en lugar de coordenadas normalizadas.
            if (cx > 1.5f || cy > 1.5f || w > 1.5f || h > 1.5f) {
                val s = inputSize.toFloat()
                cx /= s; cy /= s; w /= s; h /= s
            }
            val box = BoundingBox(
                left = (cx - w / 2f).coerceIn(0f, 1f),
                top = (cy - h / 2f).coerceIn(0f, 1f),
                right = (cx + w / 2f).coerceIn(0f, 1f),
                bottom = (cy + h / 2f).coerceIn(0f, 1f),
            )
            if (box.area <= 0f) continue
            candidates += Detection(bestClass, labels[bestClass], bestScore, box)
        }
        return NonMaxSuppression.apply(candidates, iouThreshold, maxDetections)
    }
}
