package com.tangem.tap.features.details.ui.resetcard

import com.tangem.tap.features.details.ui.cardsettings.TextReference

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
        val lastWarningDialog: LastWarningDialog,
    ) : ResetCardScreenState() {
        val resetButtonEnabled: Boolean
            get() = accepted

        data class LastWarningDialog(
            val isShown: Boolean,
            val onResetButtonClick: () -> Unit,
            val onDismiss: () -> Unit,
        )
    }

    internal enum class WarningsToReset {
        LOST_WALLET_ACCESS, LOST_PASSWORD_RESTORE
    }
}