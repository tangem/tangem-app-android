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
        // TODO
    }

    override fun onCloseAlreadySignedHashesWarningClick() {
        // TODO
    }

    override fun onGenerateMissedAddressesClick(missedAddressCurrencies: List<CryptoCurrency>) {
        // TODO
    }

    override fun onOpenUnlockWalletsBottomSheetClick() {
        // TODO
    }

    override fun onUnlockWalletClick() {
        // TODO
    }

    override fun onScanToUnlockWalletClick() {
        // TODO
    }

    override fun onLikeAppClick() {
        // TODO
    }

    override fun onDislikeAppClick() {
        // TODO
    }

    override fun onCloseRateAppWarningClick() {
        // TODO
    }
}