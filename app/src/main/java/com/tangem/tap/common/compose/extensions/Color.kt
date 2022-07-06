package com.tangem.tap.common.compose.extensions

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.core.graphics.alpha
import androidx.core.graphics.blue
import androidx.core.graphics.green
import androidx.core.graphics.red

/**
 * Created by Anton Zhilenkov on 17/03/2022.
 */
fun Color.toAndroidGraphicsColor(): Int {
    val argb = this.toArgb()
    return android.graphics.Color.argb(argb.alpha, argb.red, argb.green, argb.blue)
}

fun Color.parse(hexColor: String): Color {
    return Color(hexColor.removePrefix("#").toInt(16))
}
