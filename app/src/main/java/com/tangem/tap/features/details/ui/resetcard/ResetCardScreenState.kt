package com.tangem.tap.features.details.ui.resetcard

import androidx.annotation.StringRes
import com.tangem.tap.features.details.ui.cardsettings.TextReference
import com.tangem.wallet.R

internal sealed class ResetCardScreenState {

    object InitialState : ResetCardScreenState()

    data class ResetCardScreenContent(
        val accepted: Boolean = false,
        val descriptionText: TextReference,
        val warningsToShow: List<WarningsToReset>,
        val acceptCondition1Checked: Boolean = false,
        val acceptCondition2Checked: Boolean = false,
        val onAcceptCondition1ToggleClick: (Boolean) -> Unit,
        val onAcceptCondition2ToggleClick: (Boolean) -> Unit,
        val onResetButtonClick: () -> Unit,
        val dialog: Dialog? = null,
    ) : ResetCardScreenState() {
        val resetButtonEnabled: Boolean
            get() = accepted

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
    }

    internal enum class WarningsToReset {
        LOST_WALLET_ACCESS, LOST_PASSWORD_RESTORE
    }
}
