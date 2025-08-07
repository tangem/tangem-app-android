package com.tangem.core.ui.components.label.entity

import com.tangem.core.ui.extensions.TextReference

data class LabelUM(
    val text: TextReference,
    val style: LabelStyle,
)

enum class LabelStyle {
    REGULAR, ACCENT, WARNING,
}