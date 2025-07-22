package com.tangem.common.ui.navigationButtons

import androidx.annotation.DrawableRes
import androidx.compose.runtime.Immutable
import com.tangem.core.ui.extensions.TextReference

@Immutable
sealed class NavigationUM {
    data class Content(
        val title: TextReference,
        val subtitle: TextReference?,
        @DrawableRes val backIconRes: Int,
        val backIconClick: () -> Unit,
        @DrawableRes val additionalIconRes: Int? = null,
        val additionalIconClick: (() -> Unit)? = null,
        val primaryButton: NavigationButton,
        val prevButton: NavigationButton? = null,
        val secondaryPairButtonsUM: Pair<NavigationButton, NavigationButton>? = null,
    ) : NavigationUM()

    data object Empty : NavigationUM()
}