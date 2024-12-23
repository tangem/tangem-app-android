package com.tangem.tap.common.extensions

import com.tangem.blockchain.common.BlockchainSdkError
import com.tangem.blockchain.common.Wallet
import com.tangem.blockchain.common.WalletManager
import com.tangem.blockchain.common.address.AddressType
import com.tangem.blockchainsdk.utils.amountToCreateAccount
import com.tangem.common.services.Result
import com.tangem.domain.tokens.model.CryptoCurrency
import com.tangem.tap.common.TestActions
import com.tangem.tap.common.apptheme.MutableAppThemeModeHolder
import com.tangem.tap.domain.TapError
import com.tangem.tap.domain.getFirstToken
import com.tangem.tap.domain.model.WalletAddressData
import com.tangem.tap.network.exchangeServices.CurrencyExchangeManager
import com.tangem.tap.proxy.redux.DaggerGraphState
import com.tangem.tap.store
import kotlinx.coroutines.delay
import timber.log.Timber

/**
[REDACTED_AUTHOR]
 */
@Deprecated(
    message = "Use WalletStoresManager.fetch({userWalletId}, refresh = true) (to update all user wallet tokens)" +
        "or WalletCurrenciesManager.update(...) (to update only one user wallet blockchain and its tokens) instead",
)
@Suppress("MagicNumber")
suspend fun WalletManager.safeUpdate(isDemoCard: Boolean): Result<Wallet> = try {
    if (isDemoCard || TestActions.testAmountInjectionForWalletManagerEnabled) {
        delay(500)
        TestActions.testAmountInjectionForWalletManagerEnabled = false
        Result.Success(wallet)
    } else {
        update()
        Result.Success(wallet)
    }
} catch (exception: Exception) {
    Timber.e(exception)

    val networkConnectionManager = store.inject(DaggerGraphState::networkConnectionManager)
    if (!networkConnectionManager.isOnline) {
        Result.Failure(TapError.NoInternetConnection)
    } else {
        val blockchain = wallet.blockchain
        val amountToCreateAccount = blockchain.amountToCreateAccount(this, wallet.getFirstToken())

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

internal fun WalletManager.getTopUpUrl(cryptoCurrency: CryptoCurrency): String? {
    val globalState = store.state.globalState
    val defaultAddress = wallet.address

    return globalState.exchangeManager.getUrl(
        action = CurrencyExchangeManager.Action.Buy,
        cryptoCurrency = cryptoCurrency,
        fiatCurrencyName = globalState.appCurrency.code,
        walletAddress = defaultAddress,
        isDarkTheme = MutableAppThemeModeHolder.isDarkThemeActive,
    )
}

internal fun WalletManager?.getAddressData(): WalletAddressData? {
    val wallet = this?.wallet ?: return null

    val addressDataList = wallet.createAddressesData()
    return if (addressDataList.isEmpty()) null else addressDataList[0]
}

private fun Wallet.createAddressesData(): List<WalletAddressData> {
    val listOfAddressData = mutableListOf<WalletAddressData>()
    // put a defaultAddress at the first place
    addresses.forEach {
        val addressData = WalletAddressData(
            it.value,
            it.type,
            getShareUri(it.value),
            getExploreUrl(it.value),
        )
        if (it.type == AddressType.Default) {
            listOfAddressData.add(0, addressData)
        } else {
            listOfAddressData.add(addressData)
        }
    }
    return listOfAddressData
}