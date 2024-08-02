package com.tangem.core.ui.components.showcase.model

import androidx.annotation.DrawableRes
import com.tangem.core.ui.extensions.TextReference

data class ShowcaseItemModel(
    @DrawableRes val iconRes: Int,
    val text: TextReference,
)
