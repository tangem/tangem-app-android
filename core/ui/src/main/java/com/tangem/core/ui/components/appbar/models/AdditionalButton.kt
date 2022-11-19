package com.tangem.core.ui.components.appbar.models

import androidx.annotation.DrawableRes

data class AdditionalButton(
    @DrawableRes val iconRes: Int,
    val onIconClicked: () -> Unit,
)
