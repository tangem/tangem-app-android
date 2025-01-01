package com.tangem.core.ui.components.notifications

import androidx.annotation.DrawableRes
import com.tangem.core.ui.extensions.TextReference

/**
 * Notification component state
 *
 * @property subtitle        subtitle
 * @property iconResId       icon resource id
 * @property title           title
 * @property backgroundResId background resource id
 * @property buttonsState    buttons state
 * @property onClick         lambda be invoked when notification is clicked
 * @property onCloseClick    lambda be invoked when close button is clicked
 *
[REDACTED_AUTHOR]
 */
data class NotificationConfig(
    val subtitle: TextReference,
    @DrawableRes val iconResId: Int,
    val title: TextReference? = null,
    @DrawableRes val backgroundResId: Int? = null,
    val buttonsState: ButtonsState? = null,
    val onClick: (() -> Unit)? = null,
    val onCloseClick: (() -> Unit)? = null,
    val showArrowIcon: Boolean = onClick != null,
) {

    sealed class ButtonsState {

        data class PrimaryButtonConfig(
            val text: TextReference,
            val additionalText: TextReference? = null,
            @DrawableRes val iconResId: Int? = null,
            val onClick: () -> Unit,
        ) : ButtonsState()

        data class SecondaryButtonConfig(
            val text: TextReference,
            @DrawableRes val iconResId: Int? = null,
            val onClick: () -> Unit,
        ) : ButtonsState()

        data class PairButtonsConfig(
            val primaryText: TextReference,
            val onPrimaryClick: () -> Unit,
            val secondaryText: TextReference,
            val onSecondaryClick: () -> Unit,
        ) : ButtonsState()

        data class SecondaryPairButtonsConfig(
            val leftText: TextReference,
            val onLeftClick: () -> Unit,
            val rightText: TextReference,
            val onRightClick: () -> Unit,
        ) : ButtonsState()
    }
}