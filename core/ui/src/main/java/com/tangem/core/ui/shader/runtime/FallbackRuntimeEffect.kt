package com.tangem.core.ui.shader.runtime

import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color

internal class FallbackRuntimeEffect : RuntimeEffect {
    override val isSupported: Boolean = false
    override val isReady: Boolean = false

    override fun build(): Brush {
        return Brush.horizontalGradient(listOf(Color.White, Color.White))
    }
}