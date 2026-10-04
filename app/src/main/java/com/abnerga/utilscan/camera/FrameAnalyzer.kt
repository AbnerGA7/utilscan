package com.abnerga.utilscan.camera

import android.graphics.Bitmap
import android.graphics.Matrix
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy

/**
 * Convierte cada frame de CameraX (RGBA_8888) en un [Bitmap] ya rotado en vertical
 * y se lo entrega a [onFrame]. Se ejecuta en el executor de análisis.
 */
class FrameAnalyzer(
    private val onFrame: (Bitmap) -> Unit,
) : ImageAnalysis.Analyzer {

    override fun analyze(image: ImageProxy) {
        image.use { proxy ->
            val raw = proxy.toBitmap()
            val rotation = proxy.imageInfo.rotationDegrees
            val upright = if (rotation == 0) raw else {
                val matrix = Matrix().apply { postRotate(rotation.toFloat()) }
                Bitmap.createBitmap(raw, 0, 0, raw.width, raw.height, matrix, true).also { raw.recycle() }
            }
            onFrame(upright)
        }
    }
}
