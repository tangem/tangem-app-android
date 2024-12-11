package com.tangem.feature.tokendetails.presentation.tokendetails.state.components

import com.tangem.core.ui.extensions.TextReference
import com.tangem.core.ui.extensions.wrappedList
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

        abstract val title: TextReference?
        abstract val message: TextReference
        abstract val confirmButtonConfig: ButtonConfig
        abstract val cancelButtonConfig: ButtonConfig?

        data class ButtonConfig(
            val text: TextReference,
            val onClick: () -> Unit,
            val warning: Boolean = false,
        )

        data class ConfirmHideConfig(
            val currencyTitle: String,
            val onConfirmClick: () -> Unit,
            val onCancelClick: () -> Unit,
        ) : DialogContentConfig() {
            override val title: TextReference = TextReference.Res(
                id = R.string.token_details_hide_alert_title,
                formatArgs = wrappedList(currencyTitle),
            )

            override val message: TextReference = TextReference.Res(R.string.token_details_hide_alert_message)

            override val cancelButtonConfig: ButtonConfig = ButtonConfig(
                text = TextReference.Res(R.string.common_cancel),
                onClick = onCancelClick,
            )

            override val confirmButtonConfig: ButtonConfig = ButtonConfig(
                text = TextReference.Res(R.string.token_details_hide_alert_hide),
                onClick = onConfirmClick,
                warning = true,
            )
        }

        data class HasLinkedTokensConfig(
            val currencyName: String,
            val currencySymbol: String,
            val networkName: String,
            val onConfirmClick: () -> Unit,
        ) : DialogContentConfig() {
            override val title: TextReference = TextReference.Res(
                id = R.string.token_details_unable_hide_alert_title,
                formatArgs = wrappedList(currencySymbol),
            )

            override val message: TextReference = TextReference.Res(
                id = R.string.token_details_unable_hide_alert_message,
                formatArgs = wrappedList(currencyName, currencySymbol, networkName),
            )

            override val cancelButtonConfig: ButtonConfig?
                get() = null

            override val confirmButtonConfig: ButtonConfig = ButtonConfig(
                text = TextReference.Res(R.string.common_ok),
                onClick = onConfirmClick,
            )
        }

        data class DisabledButtonReasonDialogConfig(
            val text: TextReference,
            val onConfirmClick: () -> Unit,
        ) : DialogContentConfig() {

            override val title = null

            override val message: TextReference = text

            override val cancelButtonConfig = null

            override val confirmButtonConfig: ButtonConfig = ButtonConfig(
                text = TextReference.Res(R.string.common_ok),
                onClick = onConfirmClick,
            )
        }

        data class RemoveIncompleteTransactionConfirmDialogConfig(
            val onConfirmClick: () -> Unit,
            val onCancelClick: () -> Unit,
        ) : DialogContentConfig() {
            override val title = null

            override val message: TextReference = TextReference.Res(
                id = R.string.warning_kaspa_unfinished_token_transaction_discard_message,
            )

            override val cancelButtonConfig: ButtonConfig = ButtonConfig(
                text = TextReference.Res(R.string.common_cancel),
                onClick = onCancelClick,
            )

            override val confirmButtonConfig: ButtonConfig = ButtonConfig(
                text = TextReference.Res(R.string.common_yes),
                onClick = onConfirmClick,
            )
        }

        data class ErrorDialogConfig(
            val text: TextReference,
            val onConfirmClick: () -> Unit,
        ) : DialogContentConfig() {

            override val title = null

            override val message: TextReference = text

            override val cancelButtonConfig = null

            override val confirmButtonConfig: ButtonConfig = ButtonConfig(
                text = TextReference.Res(R.string.common_ok),
                onClick = onConfirmClick,
            )
        }
    }
}