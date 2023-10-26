package com.tangem.managetokens.presentation.managetokens.state

import androidx.compose.ui.graphics.Color
import com.tangem.core.ui.extensions.ImageReference

internal data class TokenIconState(
    val iconReference: ImageReference?,
    val placeholderTint: Color,
    val placeholderBackground: Color,
)