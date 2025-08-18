package com.tangem.common.ui.navigationButtons

import androidx.annotation.DrawableRes
import com.tangem.core.ui.extensions.TextReference

sealed class NavigationButtonsState {
    data object Empty : NavigationButtonsState()

    data class Data(
        val primaryButton: NavigationButton?,
        val prevButton: NavigationButton?,
        val extraButtons: Pair<NavigationButton, NavigationButton>?,
        val txUrl: String? = null,
        val onTextClick: (String) -> Unit,
    ) : NavigationButtonsState()
}

/**
 * @property textReference      text
 * @property iconRes            icon resource id
 * @property isSecondary        should set secondary color scheme
 * @property isIconVisible      determines whether icon is visible
 * @property showProgress       indicates progress state of button
 * @property isEnabled          enabled
 * @property isDimmed           determines whether the button content will be dimmed.
 * This property will be ignored if [isEnabled] is `false`.
 * @property onClick            lambda be invoked when action component is clicked
 */
data class NavigationButton(
    val textReference: TextReference,
    @DrawableRes val iconRes: Int? = null,
    val isSecondary: Boolean = false,
    val isIconVisible: Boolean = false,
    val showProgress: Boolean = false,
    val isEnabled: Boolean = true,
    val isDimmed: Boolean = false,
    val isHapticClick: Boolean = false,
    val onClick: () -> Unit,
)