package com.tangem.tap.common.extensions

import com.tangem.blockchain.common.BlockchainSdkError
import com.tangem.blockchain.common.Wallet
import com.tangem.blockchain.common.WalletManager
import com.tangem.common.services.Result
import com.tangem.tap.common.TestActions
import com.tangem.tap.domain.TapError
import com.tangem.tap.domain.extensions.amountToCreateAccount
import com.tangem.tap.domain.getFirstToken
import com.tangem.tap.features.demo.isDemoCard
import com.tangem.tap.features.wallet.redux.AddressData
import com.tangem.tap.features.wallet.redux.reducers.createAddressesData
import com.tangem.tap.network.NetworkConnectivity
import com.tangem.tap.network.exchangeServices.CurrencyExchangeManager
import com.tangem.tap.store
import kotlinx.coroutines.delay
import timber.log.Timber

/**
* [REDACTED_AUTHOR]
 */
suspend fun WalletManager.safeUpdate(): Result<Wallet> = try {
    val scanResponse = store.state.globalState.scanResponse
        ?: error("Scan response must not be null")

    if (scanResponse.isDemoCard() || TestActions.testAmountInjectionForWalletManagerEnabled) {
        delay(500)
        TestActions.testAmountInjectionForWalletManagerEnabled = false
        Result.Success(wallet)
    } else {
        update()
        Result.Success(wallet)
    }
} catch (exception: Exception) {
    Timber.e(exception)

    if (!NetworkConnectivity.getInstance().isOnlineOrConnecting()) {
        Result.Failure(TapError.NoInternetConnection)
    } else {
        val blockchain = wallet.blockchain
        val amountToCreateAccount = blockchain.amountToCreateAccount(wallet.getFirstToken())

        if (exception is BlockchainSdkError.AccountNotFound && amountToCreateAccount != null) {
            Result.Failure(TapError.WalletManager.NoAccountError(amountToCreateAccount.toString()))
        } else {
            val message = exception.localizedMessage ?: "An error has occurred. Try later"
            Result.Failure(TapError.WalletManager.InternalError(message))
        }
    }
}

fun WalletManager?.getToUpUrl(): String? {
    val globalState = store.state.globalState
    val exchangeManager = globalState.exchangeManager ?: return null
    val wallet = this?.wallet ?: return null

    val defaultAddress = wallet.address
    return exchangeManager.getUrl(
        action = CurrencyExchangeManager.Action.Buy,
        blockchain = wallet.blockchain,
        cryptoCurrencyName = wallet.blockchain.currency,
        fiatCurrencyName = globalState.appCurrency.code,
        walletAddress = defaultAddress,
    )
}

fun WalletManager?.getAddressData(): AddressData? {
    val wallet = this?.wallet ?: return null

    val addressDataList = wallet.createAddressesData()
    return if (addressDataList.isEmpty()) null
    else addressDataList[0]
}
