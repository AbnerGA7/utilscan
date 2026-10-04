package com.abnerga.utilscan.ui

import android.app.Application
import android.graphics.Bitmap
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import com.abnerga.utilscan.data.Accelerator
import com.abnerga.utilscan.data.ModelSpec
import com.abnerga.utilscan.data.YoloDetector
import com.abnerga.utilscan.domain.FrameResult
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

data class DetectionUiState(
    val models: List<ModelSpec> = emptyList(),
    val selectedModel: ModelSpec? = null,
    val requestedAccelerator: Accelerator = Accelerator.CPU,
    val activeAccelerator: Accelerator? = null,
    val scoreThreshold: Float = 0.35f,
    val onlySchoolSupplies: Boolean = true,
    val result: FrameResult? = null,
    val fps: Float = 0f,
    val isLoadingModel: Boolean = true,
    val error: String? = null,
)

/**
 * Orquesta el detector: toda la inferencia ocurre en [analysisExecutor], un único hilo
 * que CameraX también usa para entregar los frames, así el detector nunca se usa en paralelo.
 */
class DetectionViewModel(application: Application) : AndroidViewModel(application) {

    val analysisExecutor: ExecutorService = Executors.newSingleThreadExecutor()

    private val _state = MutableStateFlow(DetectionUiState())
    val state: StateFlow<DetectionUiState> = _state.asStateFlow()

    // Solo se accede desde analysisExecutor.
    private var detector: YoloDetector? = null
    private var lastFrameAt = 0L

    init {
        val models = ModelSpec.available(application)
        _state.update {
            it.copy(
                models = models,
                selectedModel = models.firstOrNull(),
                error = if (models.isEmpty()) "No hay modelos en assets/models. Revisa el README." else null,
                isLoadingModel = models.isNotEmpty(),
            )
        }
        reloadDetector()
    }

    fun selectModel(spec: ModelSpec) {
        if (spec == _state.value.selectedModel) return
        _state.update { it.copy(selectedModel = spec) }
        reloadDetector()
    }

    fun setAccelerator(accelerator: Accelerator) {
        if (accelerator == _state.value.requestedAccelerator) return
        _state.update { it.copy(requestedAccelerator = accelerator) }
        reloadDetector()
    }

    fun setOnlySchoolSupplies(only: Boolean) {
        if (only == _state.value.onlySchoolSupplies) return
        _state.update { it.copy(onlySchoolSupplies = only) }
        reloadDetector()
    }

    fun setScoreThreshold(threshold: Float) {
        _state.update { it.copy(scoreThreshold = threshold) }
    }

    /** Llamado por CameraX en [analysisExecutor] para cada frame. */
    fun onFrame(bitmap: Bitmap) {
        val current = detector
        if (current == null) {
            bitmap.recycle()
            return
        }
        try {
            val result = current.detect(bitmap, _state.value.scoreThreshold)
            val now = System.nanoTime()
            val instantFps = if (lastFrameAt == 0L) 0f else 1e9f / (now - lastFrameAt)
            lastFrameAt = now
            _state.update {
                // Media móvil exponencial para que el contador de FPS no salte.
                val fps = if (it.fps == 0f) instantFps else it.fps * 0.8f + instantFps * 0.2f
                it.copy(result = result, fps = fps)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error en la inferencia", e)
            _state.update { it.copy(error = e.message) }
        } finally {
            bitmap.recycle()
        }
    }

    private fun reloadDetector() {
        val snapshot = _state.value
        val spec = snapshot.selectedModel ?: return
        _state.update { it.copy(isLoadingModel = true, result = null) }
        analysisExecutor.execute {
            detector?.close()
            detector = null
            try {
                val created = YoloDetector(
                    context = getApplication(),
                    spec = spec,
                    requestedAccelerator = snapshot.requestedAccelerator,
                    onlySchoolSupplies = snapshot.onlySchoolSupplies,
                )
                detector = created
                lastFrameAt = 0L
                _state.update {
                    it.copy(isLoadingModel = false, activeAccelerator = created.accelerator, error = null, fps = 0f)
                }
            } catch (e: Exception) {
                Log.e(TAG, "No se pudo cargar el modelo ${spec.id}", e)
                _state.update { it.copy(isLoadingModel = false, error = "No se pudo cargar el modelo: ${e.message}") }
            }
        }
    }

    override fun onCleared() {
        analysisExecutor.execute {
            detector?.close()
            detector = null
        }
        analysisExecutor.shutdown()
    }

    private companion object {
        const val TAG = "DetectionViewModel"
    }
}
