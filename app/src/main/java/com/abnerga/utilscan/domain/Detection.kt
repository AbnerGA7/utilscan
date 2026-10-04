package com.abnerga.utilscan.domain

/**
 * Caja delimitadora con coordenadas normalizadas en el rango [0, 1]
 * relativas a la imagen analizada.
 */
data class BoundingBox(
    val left: Float,
    val top: Float,
    val right: Float,
    val bottom: Float,
) {
    val width: Float get() = right - left
    val height: Float get() = bottom - top
    val area: Float get() = width.coerceAtLeast(0f) * height.coerceAtLeast(0f)

    /** Intersection over Union con otra caja. */
    fun iou(other: BoundingBox): Float {
        val interLeft = maxOf(left, other.left)
        val interTop = maxOf(top, other.top)
        val interRight = minOf(right, other.right)
        val interBottom = minOf(bottom, other.bottom)
        val inter = (interRight - interLeft).coerceAtLeast(0f) * (interBottom - interTop).coerceAtLeast(0f)
        val union = area + other.area - inter
        return if (union <= 0f) 0f else inter / union
    }
}

/** Un objeto detectado en un frame. */
data class Detection(
    val classIndex: Int,
    val label: String,
    val score: Float,
    val box: BoundingBox,
)

/** Resultado de procesar un frame completo. */
data class FrameResult(
    val detections: List<Detection>,
    val inferenceTimeMs: Long,
    val imageWidth: Int,
    val imageHeight: Int,
)
