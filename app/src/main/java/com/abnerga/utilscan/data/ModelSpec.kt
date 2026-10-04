package com.abnerga.utilscan.data

import android.content.Context

/** Un modelo YOLO empaquetado en `assets/models/`. */
data class ModelSpec(
    val id: String,
    val displayName: String,
    val modelAsset: String,
    val labelsAsset: String,
) {
    companion object {
        private const val DIR = "models"

        /** Modelo afinado por ti con el notebook de `training/` (tiene prioridad si existe). */
        val CUSTOM = ModelSpec(
            id = "custom",
            displayName = "Útiles escolares (entrenado)",
            modelAsset = "$DIR/school_supplies.tflite",
            labelsAsset = "$DIR/school_supplies_labels.txt",
        )

        /** YOLOv8s pre-entrenado en Open Images V7 (601 clases), filtrado a útiles escolares. */
        val OPEN_IMAGES = ModelSpec(
            id = "oiv7",
            displayName = "YOLOv8s Open Images V7",
            modelAsset = "$DIR/yolov8s_oiv7.tflite",
            labelsAsset = "$DIR/yolov8s_oiv7_labels.txt",
        )

        /** Modelos presentes en el APK, en orden de preferencia. */
        fun available(context: Context): List<ModelSpec> {
            val files = context.assets.list(DIR).orEmpty().toSet()
            return listOf(CUSTOM, OPEN_IMAGES).filter {
                it.modelAsset.substringAfter('/') in files && it.labelsAsset.substringAfter('/') in files
            }
        }
    }
}
