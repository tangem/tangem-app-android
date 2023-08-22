package com.tangem.feature.wallet.presentation.wallet.viewmodels

import com.tangem.core.ui.components.transactions.intents.TxHistoryClickIntents
import com.tangem.domain.tokens.models.CryptoCurrency
import com.tangem.domain.wallets.models.UserWalletId

@Suppress("TooManyFunctions")
internal interface WalletClickIntents : TxHistoryClickIntents {

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

    fun onUnlockWalletClick()

    fun onUnlockWalletNotificationClick()

    fun onBottomSheetDismiss()

    fun onTokenClick(currency: CryptoCurrency)

    fun onRenameClick(userWalletId: UserWalletId, name: String)

    fun onDeleteClick(userWalletId: UserWalletId)
}