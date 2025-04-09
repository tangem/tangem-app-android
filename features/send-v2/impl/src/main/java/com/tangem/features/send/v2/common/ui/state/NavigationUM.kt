package com.tangem.features.send.v2.common.ui.state

import androidx.annotation.DrawableRes
import androidx.compose.runtime.Immutable
import com.tangem.core.ui.extensions.TextReference
import com.tangem.features.send.v2.send.ui.state.ButtonsUM

@Immutable
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