package com.abnerga.utilscan.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

val Mint = Color(0xFF4ADE80)
val Pencil = Color(0xFFFACC15)
val Navy = Color(0xFF1B2A4A)
val Ink = Color(0xFF0B1220)

/** Colores para las cajas de detección, uno por clase. */
val BoxPalette = listOf(
    Color(0xFF4ADE80), Color(0xFFFACC15), Color(0xFF60A5FA), Color(0xFFF472B6),
    Color(0xFFFB923C), Color(0xFFA78BFA), Color(0xFF2DD4BF), Color(0xFFF87171),
)

private val Scheme = darkColorScheme(
    primary = Mint,
    onPrimary = Ink,
    secondary = Pencil,
    onSecondary = Ink,
    background = Ink,
    surface = Navy,
    onSurface = Color.White,
    onBackground = Color.White,
)

@Composable
fun UtilScanTheme(content: @Composable () -> Unit) {
    MaterialTheme(colorScheme = Scheme, content = content)
}
