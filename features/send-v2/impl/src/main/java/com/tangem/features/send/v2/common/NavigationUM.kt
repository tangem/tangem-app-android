package com.tangem.features.send.v2.common

import androidx.annotation.DrawableRes
import com.tangem.core.ui.extensions.TextReference
import com.tangem.features.send.v2.send.ui.state.ButtonsUM

internal sealed class NavigationUM {
    data class Content(
        val title: TextReference,
        val subtitle: TextReference?,
        @DrawableRes val backIconRes: Int,
        val backIconClick: () -> Unit,
        @DrawableRes val additionalIconRes: Int? = null,
        val additionalIconClick: (() -> Unit)? = null,
        val primaryButton: ButtonsUM.PrimaryButtonUM,
        val prevButton: ButtonsUM.PrimaryButtonUM?,
        val secondaryPairButtonsUM: ButtonsUM.SecondaryPairButtonsUM?,
    ) : NavigationUM()

    data object Empty : NavigationUM()
}
