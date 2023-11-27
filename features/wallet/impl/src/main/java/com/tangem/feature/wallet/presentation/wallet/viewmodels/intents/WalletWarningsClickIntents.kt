package com.tangem.feature.wallet.presentation.wallet.viewmodels.intents

import com.tangem.domain.tokens.model.CryptoCurrency
import javax.inject.Inject

internal interface WalletWarningsClickIntents {

    fun onAddBackupCardClick()

    fun onCloseAlreadySignedHashesWarningClick()

    fun onGenerateMissedAddressesClick(missedAddressCurrencies: List<CryptoCurrency>)

    fun onOpenUnlockWalletsBottomSheetClick()

    fun onUnlockWalletClick()

    fun onScanToUnlockWalletClick()

    fun onLikeAppClick()

    fun onDislikeAppClick()

    fun onCloseRateAppWarningClick()
}

internal class WalletWarningsClickIntentsImplementer @Inject constructor() : WalletWarningsClickIntents {

    override fun onAddBackupCardClick() {
// [REDACTED_TODO_COMMENT]
    }

    override fun onCloseAlreadySignedHashesWarningClick() {
// [REDACTED_TODO_COMMENT]
    }

    override fun onGenerateMissedAddressesClick(missedAddressCurrencies: List<CryptoCurrency>) {
// [REDACTED_TODO_COMMENT]
    }

    override fun onOpenUnlockWalletsBottomSheetClick() {
// [REDACTED_TODO_COMMENT]
    }

    override fun onUnlockWalletClick() {
// [REDACTED_TODO_COMMENT]
    }

    override fun onScanToUnlockWalletClick() {
// [REDACTED_TODO_COMMENT]
    }

    override fun onLikeAppClick() {
// [REDACTED_TODO_COMMENT]
    }

    override fun onDislikeAppClick() {
// [REDACTED_TODO_COMMENT]
    }

    override fun onCloseRateAppWarningClick() {
// [REDACTED_TODO_COMMENT]
    }
}
