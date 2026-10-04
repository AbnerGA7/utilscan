package com.abnerga.utilscan.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AssistChip
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.abnerga.utilscan.data.Accelerator
import com.abnerga.utilscan.domain.SchoolSupplyCatalog

/** Panel inferior: resumen de útiles detectados + ajustes del detector. */
@Composable
fun DetectionPanel(
    state: DetectionUiState,
    onThresholdChange: (Float) -> Unit,
    onOnlySchoolChange: (Boolean) -> Unit,
    onAcceleratorChange: (Accelerator) -> Unit,
    onModelChange: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    var showSettings by rememberSaveable { mutableStateOf(false) }

    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.92f),
    ) {
        Column(Modifier.padding(horizontal = 16.dp, vertical = 12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                val count = state.result?.detections?.size ?: 0
                Text(
                    text = if (count == 0) "Apunta a tus útiles escolares" else "Detectados: $count",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.weight(1f),
                )
                TextButton(onClick = { showSettings = !showSettings }) {
                    Text(if (showSettings) "Ocultar ajustes" else "Ajustes")
                }
            }

            // Resumen agrupado: "✏️ Lápiz ×2"
            val grouped = state.result?.detections.orEmpty()
                .groupingBy { SchoolSupplyCatalog.displayName(it.label) }
                .eachCount()
                .toList()
                .sortedByDescending { it.second }
            if (grouped.isNotEmpty()) {
                Row(
                    modifier = Modifier.horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    grouped.forEach { (name, n) ->
                        AssistChip(onClick = {}, label = { Text(if (n > 1) "$name ×$n" else name) })
                    }
                }
            }

            AnimatedVisibility(visible = showSettings) {
                Column(Modifier.padding(top = 8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Confianza mínima: ${(state.scoreThreshold * 100).toInt()}%")
                    Slider(
                        value = state.scoreThreshold,
                        onValueChange = onThresholdChange,
                        valueRange = 0.1f..0.9f,
                    )

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("Solo útiles escolares", Modifier.weight(1f))
                        Switch(checked = state.onlySchoolSupplies, onCheckedChange = onOnlySchoolChange)
                    }

                    Text("Acelerador")
                    SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth()) {
                        Accelerator.entries.forEachIndexed { index, accelerator ->
                            SegmentedButton(
                                selected = state.requestedAccelerator == accelerator,
                                onClick = { onAcceleratorChange(accelerator) },
                                shape = SegmentedButtonDefaults.itemShape(index, Accelerator.entries.size),
                            ) { Text(accelerator.name) }
                        }
                    }

                    if (state.models.size > 1) {
                        Text("Modelo")
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            state.models.forEachIndexed { index, spec ->
                                FilterChip(
                                    selected = spec == state.selectedModel,
                                    onClick = { onModelChange(index) },
                                    label = { Text(spec.displayName) },
                                )
                            }
                        }
                    }
                    Spacer(Modifier.width(1.dp))
                }
            }

            state.error?.let {
                Text(it, color = Color(0xFFF87171), modifier = Modifier.padding(top = 8.dp))
            }
        }
    }
}
