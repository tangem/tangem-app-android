package com.tangem.core.ui.components.appbar.models

import androidx.annotation.DrawableRes
import com.tangem.core.ui.R

data class TopAppBarButtonUM(
    @DrawableRes val iconRes: Int,
    val onIconClicked: () -> Unit,
    val enabled: Boolean = true,
) {

    @Suppress("FunctionName")
    companion object {

        fun Back(onBackClicked: () -> Unit) = Back(true, onBackClicked)

        fun Back(enabled: Boolean = true, onBackClicked: () -> Unit) = TopAppBarButtonUM(
            iconRes = R.drawable.ic_back_24,
            onIconClicked = onBackClicked,
            enabled = enabled,
        )
    }
}