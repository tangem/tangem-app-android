package com.tangem.core.ui.components.label.entity

import androidx.annotation.DrawableRes
import com.tangem.core.ui.extensions.TextReference

data class LabelUM(
    val text: TextReference,
    val style: LabelStyle,
    @DrawableRes val icon: Int? = null,
    val onIconClick: (() -> Unit)? = null,
)

enum class LabelStyle {
    REGULAR, ACCENT, WARNING,
}