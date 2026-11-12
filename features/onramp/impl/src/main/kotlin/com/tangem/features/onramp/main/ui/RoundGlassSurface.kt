package com.tangem.features.onramp.main.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.tangem.core.ui.ds2.surface.TangemSurface

private val GlassRingBorder = BorderStroke(
    width = 1.dp,
    brush = Brush.linearGradient(
        colors = listOf(Color.White.copy(alpha = 0.12f), Color.White.copy(alpha = 0.09f)),
        start = Offset.Zero,
        end = Offset(120f, 120f),
    ),
)

private val GlassFillBrush = Brush.verticalGradient(
    listOf(Color.White.copy(alpha = 0.040f), Color.White.copy(alpha = 0.062f)),
)

@Composable
internal fun RoundGlassSurface(
    enabled: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    content: @Composable BoxScope.() -> Unit,
) {
    TangemSurface(
        modifier = modifier,
        color = Color.Transparent,
        shape = CircleShape,
        border = GlassRingBorder,
        enabled = enabled,
        onClick = onClick,
    ) {
        Box(
            modifier = Modifier.fillMaxSize().background(GlassFillBrush),
            contentAlignment = Alignment.Center,
            content = content,
        )
    }
}