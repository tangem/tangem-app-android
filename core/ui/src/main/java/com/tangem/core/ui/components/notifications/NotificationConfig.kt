package com.tangem.core.ui.components.notifications

import androidx.annotation.DrawableRes
import com.tangem.core.ui.extensions.TextReference

/**
 * Notification component state
 *
 * @property title           title
 * @property subtitle        subtitle
 * @property iconResId       icon resource id
 * @property backgroundResId background resource id
 * @property buttonsState    buttons state
 * @property onClick         lambda be invoked when notification is clicked
 * @property onCloseClick    lambda be invoked when close button is clicked
 *
[REDACTED_AUTHOR]
 */
data class NotificationConfig(
    val title: TextReference,
    val subtitle: TextReference,
    @DrawableRes val iconResId: Int,
    @DrawableRes val backgroundResId: Int? = null,
    val buttonsState: ButtonsState? = null,
    val onClick: (() -> Unit)? = null,
    val onCloseClick: (() -> Unit)? = null,
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
    }
}