package com.tangem.tap.features.details.ui.resetcard

import androidx.annotation.StringRes
import com.tangem.core.ui.extensions.TextReference
import com.tangem.wallet.R
import kotlinx.collections.immutable.ImmutableList

internal data class ResetCardScreenState(
    val isResetButtonEnabled: Boolean,
    val descriptionText: TextReference,
    val warningsToShow: ImmutableList<WarningUM>,
    val onToggleWarning: (WarningType) -> Unit,
    val onResetButtonClick: () -> Unit,
    val dialog: Dialog?,
) {

    sealed class Dialog(
        @StringRes val titleResId: Int,
        @StringRes val messageResId: Int,
    ) {

        abstract val onConfirmClick: () -> Unit
        abstract val onDismiss: () -> Unit

        data class StartReset(
            override val onConfirmClick: () -> Unit,
            override val onDismiss: () -> Unit,
        ) : Dialog(
            titleResId = R.string.common_attention,
            messageResId = R.string.card_settings_action_sheet_title,
        )

        data class ContinueReset(
            override val onConfirmClick: () -> Unit,
            override val onDismiss: () -> Unit,
        ) : Dialog(
            titleResId = R.string.card_settings_continue_reset_alert_title,
            messageResId = R.string.card_settings_continue_reset_alert_message,
        )

        data class InterruptedReset(
            override val onConfirmClick: () -> Unit,
            override val onDismiss: () -> Unit,
        ) : Dialog(
            titleResId = R.string.card_settings_interrupted_reset_alert_title,
            messageResId = R.string.card_settings_interrupted_reset_alert_message,
        )

        data class CompletedReset(override val onConfirmClick: () -> Unit) : Dialog(
            titleResId = R.string.card_settings_completed_reset_alert_title,
            messageResId = R.string.card_settings_completed_reset_alert_message,
        ) {

            override val onDismiss: () -> Unit = {}
        }
    }

    internal data class WarningUM(
        val isChecked: Boolean,
        val type: WarningType,
        val description: TextReference,
    )

    internal enum class WarningType {
        LOST_WALLET_ACCESS, LOST_PASSWORD_RESTORE, LOST_TANGEM_PAY
    }
}