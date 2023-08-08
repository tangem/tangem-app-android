package com.tangem.feature.wallet.presentation.wallet.viewmodels

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
}