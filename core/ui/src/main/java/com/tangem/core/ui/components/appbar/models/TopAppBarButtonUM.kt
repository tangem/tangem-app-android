package com.tangem.core.ui.components.appbar.models

import androidx.annotation.DrawableRes
import com.tangem.core.ui.R

data class TopAppBarButtonUM(
    @DrawableRes val iconRes: Int,
    val onIconClicked: () -> Unit,
) {

    @Suppress("FunctionName")
    companion object {

        fun Back(onBackClicked: () -> Unit) = TopAppBarButtonUM(
            iconRes = R.drawable.ic_back_24,
            onIconClicked = onBackClicked,
        )
    }
}