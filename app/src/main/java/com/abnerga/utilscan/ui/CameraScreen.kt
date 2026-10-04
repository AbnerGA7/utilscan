package com.abnerga.utilscan.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.abnerga.utilscan.camera.CameraPreview
import com.abnerga.utilscan.camera.FrameAnalyzer

@Composable
fun CameraScreen(viewModel: DetectionViewModel) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val analyzer = remember(viewModel) { FrameAnalyzer(viewModel::onFrame) }

    Box(Modifier.fillMaxSize()) {
        CameraPreview(
            analysisExecutor = viewModel.analysisExecutor,
            analyzer = analyzer,
            modifier = Modifier.fillMaxSize(),
        )
        DetectionOverlay(result = state.result, modifier = Modifier.fillMaxSize())

        StatusBar(
            state = state,
            modifier = Modifier
                .align(Alignment.TopCenter)
                .statusBarsPadding()
                .padding(12.dp),
        )

        if (state.isLoadingModel) {
            CircularProgressIndicator(Modifier.align(Alignment.Center))
        }

        DetectionPanel(
            state = state,
            onThresholdChange = viewModel::setScoreThreshold,
            onOnlySchoolChange = viewModel::setOnlySchoolSupplies,
            onAcceleratorChange = viewModel::setAccelerator,
            onModelChange = { index -> viewModel.selectModel(state.models[index]) },
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .navigationBarsPadding(),
        )
    }
}

/** Píldora superior con el modelo activo, latencia y FPS. */
@Composable
private fun StatusBar(state: DetectionUiState, modifier: Modifier = Modifier) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(50),
        color = Color.Black.copy(alpha = 0.55f),
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text("UtilScan", color = MaterialTheme.colorScheme.primary, style = MaterialTheme.typography.labelLarge)
            state.activeAccelerator?.let {
                Text(it.name, color = Color.White, style = MaterialTheme.typography.labelLarge)
            }
            state.result?.let {
                Text("${it.inferenceTimeMs} ms", color = Color.White, style = MaterialTheme.typography.labelLarge)
            }
            if (state.fps > 0f) {
                Text("%.1f FPS".format(state.fps), color = Color.White, style = MaterialTheme.typography.labelLarge)
            }
        }
    }
}
