package com.tangem.feature.wallet.presentation.wallet.viewmodels

import com.tangem.domain.tokens.model.CryptoCurrencyStatus
import com.tangem.domain.tokens.models.CryptoCurrency
import com.tangem.domain.wallets.models.UserWalletId

@Suppress("TooManyFunctions")
internal interface WalletClickIntents {

    fun onBackClick()

    fun onScanCardClick()

    fun onScanCardNotificationClick()

    fun onScanToUnlockWalletClick()

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

    fun onUnlockWalletClick()

    fun onUnlockWalletNotificationClick()

    fun onDismissBottomSheet()

    fun onTokenItemClick(currency: CryptoCurrency)

    fun onTokenItemLongClick(cryptoCurrencyStatus: CryptoCurrencyStatus)

    fun onDismissActionsBottomSheet()

    fun onRenameClick(userWalletId: UserWalletId, name: String)

    fun onDeleteClick(userWalletId: UserWalletId)

    fun onSingleCurrencySendClick(cryptoCurrencyStatus: CryptoCurrencyStatus? = null)

    fun onMultiCurrencySendClick(cryptoCurrencyStatus: CryptoCurrencyStatus)

    fun onReceiveClick(cryptoCurrencyStatus: CryptoCurrencyStatus)

    fun onSellClick(cryptoCurrencyStatus: CryptoCurrencyStatus)

    fun onManageTokensClick()

    fun onBuyClick(cryptoCurrencyStatus: CryptoCurrencyStatus)

    fun onSwapClick(cryptoCurrencyStatus: CryptoCurrencyStatus)

    fun onReloadClick()

    fun onExploreClick()
}
