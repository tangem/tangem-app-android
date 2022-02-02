package com.tangem.tap.common.extensions

import com.tangem.blockchain.common.BlockchainSdkError
import com.tangem.blockchain.common.Wallet
import com.tangem.blockchain.common.WalletManager
import com.tangem.common.services.Result
import com.tangem.tap.domain.TapError
import com.tangem.tap.domain.extensions.amountToCreateAccount
import com.tangem.tap.domain.getFirstToken
import com.tangem.tap.domain.topup.TradeCryptoHelper
import com.tangem.tap.features.wallet.redux.AddressData
import com.tangem.tap.features.wallet.redux.reducers.createAddressesData
import com.tangem.tap.network.NetworkConnectivity
import com.tangem.tap.store

/**
[REDACTED_AUTHOR]
 */
suspend fun WalletManager.safeUpdate(): Result<Wallet> = try {
    update()
    Result.Success(wallet)
} catch (exception: Exception) {
    if (!NetworkConnectivity.getInstance().isOnlineOrConnecting()) {
        Result.Failure(TapError.NoInternetConnection)
    } else {
        val blockchain = wallet.blockchain
        val amountToCreateAccount = blockchain.amountToCreateAccount(wallet.getFirstToken())

        if (exception is BlockchainSdkError.AccountNotFound && amountToCreateAccount != null) {
            Result.Failure(TapError.WalletManagerUpdate.NoAccountError(amountToCreateAccount.toString()))
        } else {
            val message = exception.localizedMessage ?: "An error has occurred. Try later"
            Result.Failure(TapError.WalletManagerUpdate.InternalError(message))
        }
    }
}

fun WalletManager?.getToUpUrl(): String? {
    val wallet = this?.wallet ?: return null
    val config = store.state.globalState.configManager?.config ?: return null
    val defaultAddress = wallet.address

    return TradeCryptoHelper.getUrl(
        TradeCryptoHelper.Action.Buy,
        wallet.blockchain,
        wallet.blockchain.currency,
        defaultAddress,
        config.moonPayApiKey,
        config.moonPayApiSecretKey
    )
}

fun WalletManager?.getAddressData(): AddressData? {
    val wallet = this?.wallet ?: return null

    val addressDataList = wallet.createAddressesData()
    return if (addressDataList.isEmpty()) null
    else addressDataList[0]
}