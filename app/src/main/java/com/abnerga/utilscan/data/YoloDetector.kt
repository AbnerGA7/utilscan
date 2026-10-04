package com.abnerga.utilscan.data

import android.content.Context
import android.graphics.Bitmap
import android.os.SystemClock
import android.util.Log
import com.abnerga.utilscan.domain.FrameResult
import com.abnerga.utilscan.domain.SchoolSupplyCatalog
import com.abnerga.utilscan.domain.YoloOutputParser
import org.tensorflow.lite.DataType
import org.tensorflow.lite.Interpreter
import org.tensorflow.lite.gpu.CompatibilityList
import org.tensorflow.lite.gpu.GpuDelegate
import java.io.Closeable
import java.io.FileInputStream
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.MappedByteBuffer
import java.nio.channels.FileChannel

enum class Accelerator { CPU, GPU }

/**
 * Detector de objetos YOLO que corre 100% en el dispositivo con LiteRT (TensorFlow Lite).
 * No es thread-safe: úsalo desde un único hilo (el executor de análisis de CameraX).
 */
class YoloDetector(
    context: Context,
    val spec: ModelSpec,
    requestedAccelerator: Accelerator,
    /** Si es `true`, solo se consideran las clases que son útiles escolares. */
    onlySchoolSupplies: Boolean,
) : Closeable {

    val labels: List<String> = context.assets.open(spec.labelsAsset).bufferedReader().useLines { lines ->
        lines.map { it.trim() }.filter { it.isNotEmpty() }.toList()
    }

    private var gpuDelegate: GpuDelegate? = null
    private val interpreter: Interpreter
    val accelerator: Accelerator

    private val inputWidth: Int
    private val inputHeight: Int
    private val outputShape: IntArray
    private val inputBuffer: ByteBuffer
    private val outputBuffer: ByteBuffer
    private val outputArray: FloatArray
    private val pixels: IntArray
    private val parser: YoloOutputParser

    init {
        val model = loadModel(context, spec.modelAsset)
        val (created, usedAccelerator) = createInterpreter(model, requestedAccelerator)
        interpreter = created
        accelerator = usedAccelerator

        val inputTensor = interpreter.getInputTensor(0)
        require(inputTensor.dataType() == DataType.FLOAT32) {
            "El modelo debe tener entrada FLOAT32 (exporta sin cuantización int8 completa)."
        }
        // Entrada NHWC: [1, alto, ancho, 3]
        val inShape = inputTensor.shape()
        inputHeight = inShape[1]
        inputWidth = inShape[2]
        outputShape = interpreter.getOutputTensor(0).shape()

        inputBuffer = ByteBuffer.allocateDirect(4 * inputWidth * inputHeight * 3).order(ByteOrder.nativeOrder())
        val outputSize = outputShape.fold(1) { acc, d -> acc * d }
        outputBuffer = ByteBuffer.allocateDirect(4 * outputSize).order(ByteOrder.nativeOrder())
        outputArray = FloatArray(outputSize)
        pixels = IntArray(inputWidth * inputHeight)

        val allowed = if (onlySchoolSupplies) SchoolSupplyCatalog.schoolClassIndices(labels) else null
        parser = YoloOutputParser(labels, allowed)
        Log.i(TAG, "Modelo ${spec.id}: entrada ${inShape.contentToString()}, salida ${outputShape.contentToString()}, $accelerator")
    }

    fun detect(bitmap: Bitmap, scoreThreshold: Float): FrameResult {
        val start = SystemClock.uptimeMillis()
        fillInput(bitmap)
        outputBuffer.rewind()
        interpreter.run(inputBuffer, outputBuffer)
        outputBuffer.rewind()
        outputBuffer.asFloatBuffer().get(outputArray)
        val detections = parser.parse(outputArray, outputShape, inputWidth, scoreThreshold)
        return FrameResult(
            detections = detections,
            inferenceTimeMs = SystemClock.uptimeMillis() - start,
            imageWidth = bitmap.width,
            imageHeight = bitmap.height,
        )
    }

    /** Redimensiona al tamaño del modelo y normaliza a [0, 1] en RGB. */
    private fun fillInput(bitmap: Bitmap) {
        val scaled = if (bitmap.width == inputWidth && bitmap.height == inputHeight) bitmap
        else Bitmap.createScaledBitmap(bitmap, inputWidth, inputHeight, true)
        scaled.getPixels(pixels, 0, inputWidth, 0, 0, inputWidth, inputHeight)
        if (scaled !== bitmap) scaled.recycle()

        inputBuffer.rewind()
        for (p in pixels) {
            inputBuffer.putFloat(((p shr 16) and 0xFF) / 255f)
            inputBuffer.putFloat(((p shr 8) and 0xFF) / 255f)
            inputBuffer.putFloat((p and 0xFF) / 255f)
        }
        inputBuffer.rewind()
    }

    private fun createInterpreter(model: MappedByteBuffer, requested: Accelerator): Pair<Interpreter, Accelerator> {
        if (requested == Accelerator.GPU) {
            try {
                val compatibility = CompatibilityList()
                if (compatibility.isDelegateSupportedOnThisDevice) {
                    val delegate = GpuDelegate(compatibility.bestOptionsForThisDevice)
                    gpuDelegate = delegate
                    val options = Interpreter.Options().addDelegate(delegate)
                    return Interpreter(model, options) to Accelerator.GPU
                }
                Log.w(TAG, "GPU no compatible en este dispositivo, usando CPU")
            } catch (e: Throwable) {
                Log.w(TAG, "No se pudo iniciar la GPU, usando CPU", e)
                gpuDelegate?.close()
                gpuDelegate = null
            }
        }
        val cpuOptions = Interpreter.Options()
            .setNumThreads(Runtime.getRuntime().availableProcessors().coerceIn(2, 4))
            .setUseXNNPACK(true)
        return Interpreter(model, cpuOptions) to Accelerator.CPU
    }

    override fun close() {
        interpreter.close()
        gpuDelegate?.close()
    }

    private companion object {
        const val TAG = "YoloDetector"

        fun loadModel(context: Context, asset: String): MappedByteBuffer =
            context.assets.openFd(asset).use { fd ->
                FileInputStream(fd.fileDescriptor).channel.use { channel ->
                    channel.map(FileChannel.MapMode.READ_ONLY, fd.startOffset, fd.declaredLength)
                }
            }
    }
}
