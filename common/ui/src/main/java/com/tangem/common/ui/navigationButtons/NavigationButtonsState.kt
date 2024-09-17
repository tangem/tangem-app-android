package com.tangem.common.ui.navigationButtons

import androidx.annotation.DrawableRes
import com.tangem.core.ui.extensions.TextReference
import kotlinx.collections.immutable.ImmutableList

sealed class NavigationButtonsState {
    data object Empty : NavigationButtonsState()

    data class Data(
        val primaryButton: NavigationButton,
        val prevButton: NavigationButton?,
        val extraButtons: ImmutableList<NavigationButton>,
        val txUrl: String? = null,
        val onTextClick: (String) -> Unit,
    ) : NavigationButtonsState()
}

data class NavigationButton(
    val textReference: TextReference,
    @DrawableRes val iconRes: Int? = null,
    val isSecondary: Boolean,
    val isIconVisible: Boolean,
    val showProgress: Boolean,
    val isEnabled: Boolean,
    val onClick: () -> Unit,
)