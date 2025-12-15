package com.tangem.feature.wallet.presentation.wallet.state.model

import androidx.annotation.DrawableRes
import androidx.compose.runtime.Immutable
import com.tangem.core.ui.extensions.TextReference

@Immutable
internal sealed class TangemPayState {

    object Empty : TangemPayState()

    data object Loading : TangemPayState()

    data class Progress(
        val title: TextReference,
        val description: TextReference,
        val buttonText: TextReference,
        @DrawableRes val iconRes: Int,
        val onButtonClick: () -> Unit,
        val showProgress: Boolean = false,
    ) : TangemPayState()

    data class FailedIssue(
        val title: TextReference,
        val description: TextReference,
        @DrawableRes val iconRes: Int,
        val onButtonClick: () -> Unit,
    ) : TangemPayState()

    data class Card(
        val lastFourDigits: TextReference,
        val balanceText: TextReference,
        val balanceSymbol: TextReference,
        val onClick: () -> Unit,
    ) : TangemPayState()

    data class RefreshNeeded(
        val notification: WalletNotification,
    ) : TangemPayState()

    data class TemporaryUnavailable(val notification: WalletNotification) : TangemPayState()
}