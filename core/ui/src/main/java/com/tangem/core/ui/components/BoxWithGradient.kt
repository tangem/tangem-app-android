package com.tangem.core.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import com.tangem.core.ui.res.TangemColorPalette
import com.tangem.core.ui.res.TangemTheme

@Composable
fun BoxWithGradient(
    modifier: Modifier = Modifier,
    gradient: Brush = BottomGradient,
    content: @Composable BoxScope.() -> Unit,
) {
    val bottomInsetsPx = WindowInsets.navigationBars.getBottom(LocalDensity.current)

    Box(modifier = modifier.fillMaxSize()) {
        content()
        Box(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .height(TangemTheme.dimens.size164 + bottomInsetsPx.dp)
                .background(gradient),
        )
    }
}

private val BottomGradient: Brush = Brush.verticalGradient(
    colors = listOf(
        TangemColorPalette.Black.copy(alpha = 0f),
        TangemColorPalette.Black.copy(alpha = 0.75f),
        TangemColorPalette.Black.copy(alpha = 0.95f),
        TangemColorPalette.Black,
    ),
)