package com.tangem.feature.wallet.presentation.wallet.viewmodels

import com.tangem.domain.tokens.models.CryptoCurrency

internal interface WalletClickIntents {

    fun onBackClick()

    fun onScanCardClick()

    fun onDetailsClick()

    fun onBackupCardClick()

    fun onCriticalWarningAlreadySignedHashesClick()

    fun onCloseWarningAlreadySignedHashesClick()

    fun onLikeTangemAppClick()

    fun onRateTheAppClick()

    fun onShareClick()

    fun onWalletChange(index: Int)

    fun onRefreshSwipe()

    fun onOrganizeTokensClick()

    fun onBuyClick()

    fun onReloadClick()

    fun onExploreClick()

    fun onUnlockWalletClick()

    fun onUnlockWalletNotificationClick()

    fun onBottomSheetDismiss()

    fun onTokenClick(currency: CryptoCurrency)
}