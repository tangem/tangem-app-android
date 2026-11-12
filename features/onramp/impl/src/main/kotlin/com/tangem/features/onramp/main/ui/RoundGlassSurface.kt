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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.tangem.core.ui.ds2.surface.TangemSurface
import com.tangem.core.ui.res.TangemTheme

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
    val materialBorder = TangemTheme.colors3.material.border
    TangemSurface(
        modifier = modifier,
        color = Color.Transparent,
        shape = CircleShape,
        border = BorderStroke(
            width = 1.dp,
            brush = Brush.linearGradient(
                0f to materialBorder.start,
                0.4f to materialBorder.mid,
                0.6f to materialBorder.mid,
                1f to materialBorder.end,
            ),
        ),
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