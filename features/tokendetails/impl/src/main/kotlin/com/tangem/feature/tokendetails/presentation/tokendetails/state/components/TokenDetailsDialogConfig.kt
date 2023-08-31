package com.tangem.feature.tokendetails.presentation.tokendetails.state.components

import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import com.tangem.core.ui.extensions.TextReference
import com.tangem.core.ui.extensions.wrappedList
import com.tangem.core.ui.res.TangemTheme
import com.tangem.features.tokendetails.impl.R

/**
 * Wallet bottom sheet config
 *
 * @property isShow           flag that determine if bottom sheet is shown
 * @property onDismissRequest lambda be invoked when bottom sheet is dismissed
 * @property content          content config
 */
internal data class TokenDetailsDialogConfig(
    val isShow: Boolean,
    val onDismissRequest: () -> Unit,
    val content: DialogContentConfig,
) {

    sealed class DialogContentConfig {

        abstract val title: TextReference
        abstract val text: TextReference
        abstract val confirmButtonConfig: ButtonConfig
        abstract val cancelButtonConfig: ButtonConfig?

        data class ButtonConfig(
            val text: TextReference,
            val textColorProvider: @Composable () -> Color,
            val onClick: () -> Unit,
        )

        data class ConfirmHideConfig(
            val currencySymbol: String,
            val onConfirmClick: () -> Unit,
            val onCancelClick: () -> Unit,
        ) : DialogContentConfig() {
            override val title: TextReference = TextReference.Res(
                id = R.string.token_details_hide_alert_title,
                formatArgs = wrappedList(currencySymbol),
            )

            override val text: TextReference = TextReference.Res(R.string.token_details_hide_alert_message)

            override val cancelButtonConfig: ButtonConfig = ButtonConfig(
                text = TextReference.Res(R.string.common_cancel),
                textColorProvider = { TangemTheme.colors.text.secondary },
                onClick = onCancelClick,
            )

            override val confirmButtonConfig: ButtonConfig = ButtonConfig(
                text = TextReference.Res(R.string.token_details_hide_alert_hide),
                textColorProvider = { TangemTheme.colors.text.warning },
                onClick = onConfirmClick,
            )
        }

        data class HasLinkedTokensConfig(
            val currencySymbol: String,
            val networkName: String,
            val onConfirmClick: () -> Unit,
        ) : DialogContentConfig() {
            override val title: TextReference = TextReference.Res(
                id = R.string.token_details_unable_hide_alert_title,
                formatArgs = wrappedList(currencySymbol),
            )

            override val text: TextReference = TextReference.Res(
                id = R.string.token_details_unable_hide_alert_message,
                formatArgs = wrappedList(currencySymbol, networkName),
            )

            override val cancelButtonConfig: ButtonConfig?
                get() = null

            override val confirmButtonConfig: ButtonConfig = ButtonConfig(
                text = TextReference.Res(R.string.common_ok),
                textColorProvider = { TangemTheme.colors.text.primary1 },
                onClick = onConfirmClick,
            )
        }
    }
}
