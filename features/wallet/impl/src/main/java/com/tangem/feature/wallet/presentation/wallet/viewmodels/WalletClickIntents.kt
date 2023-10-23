package com.tangem.feature.wallet.presentation.wallet.viewmodels

import com.tangem.domain.tokens.model.CryptoCurrency
import com.tangem.domain.tokens.model.CryptoCurrencyStatus
import com.tangem.domain.wallets.models.UserWalletId

@Suppress("TooManyFunctions")
internal interface WalletClickIntents {

    fun onBackClick()

    fun onGenerateMissedAddressesClick(missedAddressCurrencies: List<CryptoCurrency>)

    fun onScanToUnlockWalletClick()

    fun onDetailsClick()

    fun onBackupCardClick()

    fun onSignedHashesNotificationCloseClick()

    fun onLikeAppClick()

    fun onDislikeAppClick()

    fun onCloseRateAppNotificationClick()

    fun onWalletChange(index: Int)

    fun onRefreshSwipe()

    fun onOrganizeTokensClick()

    fun onUnlockWalletClick()

    fun onUnlockWalletNotificationClick()

    fun onDismissBottomSheet()

    fun onTokenItemClick(currency: CryptoCurrency)

    fun onTokenItemLongClick(cryptoCurrencyStatus: CryptoCurrencyStatus)

    fun onRenameClick(userWalletId: UserWalletId, name: String)

    fun onDeleteBeforeConfirmationClick(userWalletId: UserWalletId)

    fun onDeleteAfterConfirmationClick(userWalletId: UserWalletId)

    fun onSingleCurrencySendClick(cryptoCurrencyStatus: CryptoCurrencyStatus? = null)

    fun onMultiCurrencySendClick(cryptoCurrencyStatus: CryptoCurrencyStatus)

    fun onReceiveClick(cryptoCurrencyStatus: CryptoCurrencyStatus)

    fun onCopyAddressClick(cryptoCurrencyStatus: CryptoCurrencyStatus)

    fun onHideTokensClick(cryptoCurrencyStatus: CryptoCurrencyStatus)

    fun onPerformHideToken(cryptoCurrencyStatus: CryptoCurrencyStatus)

    fun onSellClick(cryptoCurrencyStatus: CryptoCurrencyStatus)

    fun onManageTokensClick()

    fun onBuyClick(cryptoCurrencyStatus: CryptoCurrencyStatus)

    fun onSwapClick(cryptoCurrencyStatus: CryptoCurrencyStatus)

    fun onReloadClick()

    fun onExploreClick()

    fun onTransactionClick(txHash: String)
}