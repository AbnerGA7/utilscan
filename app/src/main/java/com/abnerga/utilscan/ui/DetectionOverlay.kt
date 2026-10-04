package com.abnerga.utilscan.ui

import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.abnerga.utilscan.domain.FrameResult
import com.abnerga.utilscan.domain.SchoolSupplyCatalog
import com.abnerga.utilscan.ui.theme.BoxPalette
import kotlin.math.max

/**
 * Dibuja las detecciones encima del PreviewView. El preview usa FILL_CENTER, así que la imagen
 * se escala para cubrir la vista y se recorta por los bordes: replicamos esa transformación.
 */
@Composable
fun DetectionOverlay(result: FrameResult?, modifier: Modifier = Modifier) {
    val textMeasurer = rememberTextMeasurer()
    Canvas(modifier = modifier) {
        val frame = result ?: return@Canvas
        if (frame.imageWidth == 0 || frame.imageHeight == 0) return@Canvas

        val scale = max(size.width / frame.imageWidth, size.height / frame.imageHeight)
        val drawnWidth = frame.imageWidth * scale
        val drawnHeight = frame.imageHeight * scale
        val offsetX = (size.width - drawnWidth) / 2f
        val offsetY = (size.height - drawnHeight) / 2f
        val strokePx = 3.dp.toPx()
        val corner = CornerRadius(8.dp.toPx())
        val padding = 6.dp.toPx()

        for (detection in frame.detections) {
            val color = BoxPalette[detection.classIndex % BoxPalette.size]
            val left = offsetX + detection.box.left * drawnWidth
            val top = offsetY + detection.box.top * drawnHeight
            val width = detection.box.width * drawnWidth
            val height = detection.box.height * drawnHeight

            drawRoundRect(
                color = color,
                topLeft = Offset(left, top),
                size = Size(width, height),
                cornerRadius = corner,
                style = Stroke(width = strokePx),
            )

            val text = "${SchoolSupplyCatalog.displayName(detection.label)}  ${(detection.score * 100).toInt()}%"
            val layout = textMeasurer.measure(
                text = text,
                style = TextStyle(color = Color.Black, fontSize = 14.sp, fontWeight = FontWeight.Bold),
            )
            val labelHeight = layout.size.height + padding
            val labelTop = if (top - labelHeight > 0f) top - labelHeight else top
            drawRoundRect(
                color = color,
                topLeft = Offset(left, labelTop),
                size = Size(layout.size.width + padding * 2, labelHeight),
                cornerRadius = corner,
            )
            drawText(layout, topLeft = Offset(left + padding, labelTop + padding / 2))
        }
    }
}
