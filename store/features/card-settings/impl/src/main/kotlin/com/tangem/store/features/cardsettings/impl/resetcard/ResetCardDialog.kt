package com.tangem.store.features.cardsettings.impl.resetcard

internal sealed class ResetCardDialog {
    data object StartResetDialog : ResetCardDialog()
    data object ContinueResetDialog : ResetCardDialog()
    data object InterruptedResetDialog : ResetCardDialog()
    data object CompletedResetDialog : ResetCardDialog()
}