package com.tangem.features.tangempay

import androidx.annotation.DrawableRes
import androidx.compose.runtime.Immutable
import com.tangem.common.ui.userwallet.WalletNotification
import com.tangem.core.ui.extensions.TextReference

@Immutable
sealed class TangemPayMainBannerState {

    object Empty : TangemPayMainBannerState()

    data object Loading : TangemPayMainBannerState()

    data class OnboardingBanner(
        val onClick: () -> Unit,
        val closeOnClick: () -> Unit,
    ) : TangemPayMainBannerState()

    data class Progress(
        val title: TextReference,
        val description: TextReference,
        val buttonText: TextReference,
        @DrawableRes val iconRes: Int,
        val onButtonClick: () -> Unit,
        val shouldShowProgress: Boolean = false,
    ) : TangemPayMainBannerState()

    data class FailedIssue(
        val title: TextReference,
        val description: TextReference,
        @DrawableRes val iconRes: Int,
        val onButtonClick: () -> Unit,
    ) : TangemPayMainBannerState()

    data class Card(
        val lastFourDigits: TextReference,
        val balanceText: TextReference,
        val balanceSymbol: TextReference,
        val onClick: () -> Unit,
    ) : TangemPayMainBannerState()

    data class RefreshNeeded(
        val notification: WalletNotification,
    ) : TangemPayMainBannerState()

    data class TemporaryUnavailable(val notification: WalletNotification) : TangemPayMainBannerState()

    data object ExposedDevice : TangemPayMainBannerState()
}