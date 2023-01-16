package com.tangem.tap.common.extensions

import com.tangem.blockchain.common.Blockchain
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
 * Created by Anton Zhilenkov on 03/10/2021.
 */
@Suppress("MagicNumber")
suspend fun WalletManager.safeUpdate(): Result<Wallet> = try {
    val scanResponse = store.state.globalState.scanResponse

    if (scanResponse?.isDemoCard() == true || TestActions.testAmountInjectionForWalletManagerEnabled) {
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
            when (exception) {
                is BlockchainSdkError -> Result.Failure(exception)
                else -> {
                    val message = exception.cause?.localizedMessage ?: "Unknown error"
                    Result.Failure(TapError.WalletManager.InternalError(message))
                }
            }
        }
    }
}

fun WalletManager.getTopUpUrl(): String? {
    val globalState = store.state.globalState
    val defaultAddress = wallet.address

    return globalState.exchangeManager.getUrl(
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
    return if (addressDataList.isEmpty()) null else addressDataList[0]
}

fun <T> WalletManager.Companion.stub(): T {
    val wallet = Wallet(Blockchain.Unknown, setOf(), Wallet.PublicKey(byteArrayOf(), null, null), setOf())
    return object : WalletManager(wallet) {
        override val currentHost: String = ""
        override suspend fun update() {}
    } as T
}
