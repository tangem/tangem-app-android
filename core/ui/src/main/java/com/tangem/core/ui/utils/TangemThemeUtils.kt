package com.tangem.core.ui.utils

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.Color
import com.tangem.core.ui.res.LocalRootBackgroundColor

@Composable
fun ChangeRootBackgroundColorEffect(color: Color) {
    var rootBackgroundColor by LocalRootBackgroundColor.current
    var previousColor by remember { mutableStateOf(rootBackgroundColor) }

    DisposableEffect(color) {
        rootBackgroundColor = color
        onDispose {
            rootBackgroundColor = previousColor
        }
    }

    LaunchedEffect(rootBackgroundColor) {
        if (rootBackgroundColor != color) {
            previousColor = rootBackgroundColor
            rootBackgroundColor = color
        }
    }
}