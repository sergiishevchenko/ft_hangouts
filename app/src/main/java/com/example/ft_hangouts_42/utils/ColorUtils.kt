package com.example.ft_hangouts_42.utils

import androidx.compose.ui.graphics.Color

fun Color.toArgbInt(): Int {
    return (alpha * 255).toInt().shl(24) or
            (red * 255).toInt().shl(16) or
            (green * 255).toInt().shl(8) or
            (blue * 255).toInt()
}

fun colorFromArgbInt(argb: Int): Color {
    val alpha = ((argb shr 24) and 0xFF) / 255f
    val red = ((argb shr 16) and 0xFF) / 255f
    val green = ((argb shr 8) and 0xFF) / 255f
    val blue = (argb and 0xFF) / 255f
    return Color(red, green, blue, alpha)
}