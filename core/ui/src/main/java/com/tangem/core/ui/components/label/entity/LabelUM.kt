package com.tangem.core.ui.components.label.entity

import androidx.annotation.DrawableRes
import androidx.compose.runtime.Immutable
import com.tangem.core.ui.extensions.TextReference

data class LabelUM(
    val text: TextReference,
    val style: LabelStyle = LabelStyle.REGULAR,
    val size: LabelSize = LabelSize.REGULAR,
    val leadingContent: LabelLeadingContentUM = LabelLeadingContentUM.None,
    @DrawableRes val icon: Int? = null,
    val onIconClick: (() -> Unit)? = null,
    val onClick: (() -> Unit)? = null,
)

@Immutable
sealed class LabelLeadingContentUM {
    data object None : LabelLeadingContentUM()
    data class Token(val icon: Int) : LabelLeadingContentUM()
}

enum class LabelStyle {
    REGULAR, ACCENT, WARNING,
}

enum class LabelSize {
    REGULAR, BIG,
}