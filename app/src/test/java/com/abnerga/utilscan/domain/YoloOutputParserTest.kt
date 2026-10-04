package com.abnerga.utilscan.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class YoloOutputParserTest {

    private val labels = listOf("Pen", "Ruler", "Book")

    /** Construye una salida canales-primero `[1, 4 + nc, anchors]`. */
    private fun channelsFirst(anchors: List<FloatArray>): Pair<FloatArray, IntArray> {
        val channels = anchors.first().size
        val out = FloatArray(channels * anchors.size)
        anchors.forEachIndexed { a, values ->
            values.forEachIndexed { c, v -> out[c * anchors.size + a] = v }
        }
        return out to intArrayOf(1, channels, anchors.size)
    }

    @Test
    fun `decodifica una deteccion en formato canales primero`() {
        val (out, shape) = channelsFirst(
            listOf(
                floatArrayOf(0.5f, 0.5f, 0.2f, 0.4f, 0.1f, 0.9f, 0.0f),
                floatArrayOf(0.1f, 0.1f, 0.1f, 0.1f, 0.05f, 0.05f, 0.05f),
            )
        )
        val result = YoloOutputParser(labels).parse(out, shape, 640, scoreThreshold = 0.5f)

        assertEquals(1, result.size)
        val d = result.single()
        assertEquals("Ruler", d.label)
        assertEquals(0.9f, d.score, 1e-6f)
        assertEquals(0.4f, d.box.left, 1e-6f)
        assertEquals(0.3f, d.box.top, 1e-6f)
        assertEquals(0.6f, d.box.right, 1e-6f)
        assertEquals(0.7f, d.box.bottom, 1e-6f)
    }

    @Test
    fun `decodifica formato anchors primero`() {
        val anchor = floatArrayOf(0.5f, 0.5f, 0.2f, 0.2f, 0.0f, 0.0f, 0.8f)
        val result = YoloOutputParser(labels).parse(anchor, intArrayOf(1, 1, 7), 640, 0.5f)
        assertEquals("Book", result.single().label)
    }

    @Test
    fun `normaliza coordenadas en pixeles`() {
        val anchor = floatArrayOf(320f, 320f, 64f, 64f, 0.9f, 0f, 0f)
        val d = YoloOutputParser(labels).parse(anchor, intArrayOf(1, 1, 7), 640, 0.5f).single()
        assertEquals(0.45f, d.box.left, 1e-5f)
        assertEquals(0.55f, d.box.right, 1e-5f)
    }

    @Test
    fun `ignora clases fuera del filtro permitido`() {
        val anchor = floatArrayOf(0.5f, 0.5f, 0.2f, 0.2f, 0.95f, 0.6f, 0f)
        val d = YoloOutputParser(labels, allowedClasses = setOf(1))
            .parse(anchor, intArrayOf(1, 1, 7), 640, 0.5f)
            .single()
        assertEquals("Ruler", d.label)
        assertEquals(0.6f, d.score, 1e-6f)
    }

    @Test
    fun `nms elimina cajas solapadas de la misma clase`() {
        val (out, shape) = channelsFirst(
            listOf(
                floatArrayOf(0.50f, 0.50f, 0.2f, 0.2f, 0.9f, 0f, 0f),
                floatArrayOf(0.51f, 0.51f, 0.2f, 0.2f, 0.8f, 0f, 0f),
                floatArrayOf(0.10f, 0.10f, 0.1f, 0.1f, 0.7f, 0f, 0f),
            )
        )
        val result = YoloOutputParser(labels).parse(out, shape, 640, 0.5f)
        assertEquals(2, result.size)
        assertEquals(0.9f, result[0].score, 1e-6f)
        assertEquals(0.7f, result[1].score, 1e-6f)
    }

    @Test(expected = IllegalStateException::class)
    fun `falla si las etiquetas no coinciden con el modelo`() {
        YoloOutputParser(labels).parse(FloatArray(10), intArrayOf(1, 10, 1), 640, 0.5f)
    }

    @Test
    fun `iou de cajas identicas es 1 y de cajas disjuntas es 0`() {
        val a = BoundingBox(0f, 0f, 0.5f, 0.5f)
        val b = BoundingBox(0.6f, 0.6f, 1f, 1f)
        assertEquals(1f, a.iou(a), 1e-6f)
        assertTrue(a.iou(b) == 0f)
    }
}
