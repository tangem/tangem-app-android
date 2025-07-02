package com.tangem.features.swap.v2.impl.common.entity

import androidx.annotation.DrawableRes
import androidx.compose.runtime.Immutable
import com.tangem.common.ui.navigationButtons.NavigationButton
import com.tangem.core.ui.extensions.TextReference

@Immutable
internal sealed class NavigationUM {
    data class Content(
        val title: TextReference,
        val subtitle: TextReference?,
        @DrawableRes val backIconRes: Int,
        val backIconClick: () -> Unit,
        @DrawableRes val additionalIconRes: Int? = null,
        val additionalIconClick: (() -> Unit)? = null,
        val primaryButton: NavigationButton,
        val secondaryPairButtonsUM: Pair<NavigationButton, NavigationButton>? = null,
    ) : NavigationUM()

    data object Empty : NavigationUM()
}