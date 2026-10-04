package com.abnerga.utilscan.domain

/**
 * Non-Maximum Suppression por clase: elimina cajas solapadas que
 * representan el mismo objeto y conserva la de mayor confianza.
 */
object NonMaxSuppression {

    fun apply(
        detections: List<Detection>,
        iouThreshold: Float,
        maxDetections: Int,
        classAgnostic: Boolean = false,
    ): List<Detection> {
        val sorted = detections.sortedByDescending { it.score }
        val kept = ArrayList<Detection>(minOf(sorted.size, maxDetections))
        for (candidate in sorted) {
            if (kept.size >= maxDetections) break
            val overlaps = kept.any { keptDetection ->
                (classAgnostic || keptDetection.classIndex == candidate.classIndex) &&
                    keptDetection.box.iou(candidate.box) > iouThreshold
            }
            if (!overlaps) kept += candidate
        }
        return kept
    }
}
