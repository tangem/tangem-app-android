package com.tangem.tap.common.extensions

import com.tangem.blockchain.common.Wallet
import com.tangem.blockchain.common.WalletManager
import com.tangem.common.services.Result
import com.tangem.tap.domain.TapError
import com.tangem.tap.domain.extensions.amountToCreateAccount
import com.tangem.tap.domain.extensions.isNoAccountError
import com.tangem.tap.domain.getFirstToken
import com.tangem.tap.network.NetworkConnectivity

/**
[REDACTED_AUTHOR]
 */
suspend fun WalletManager.loadWalletData(): Result<Wallet> = try {
    update()
    Result.Success(wallet)
} catch (exception: Exception) {
    if (!NetworkConnectivity.getInstance().isOnlineOrConnecting()) {
        Result.Failure(TapError.NoInternetConnection)
    } else {
        val blockchain = wallet.blockchain
        val amountToCreateAccount = blockchain.amountToCreateAccount(wallet.getFirstToken())

        if (blockchain.isNoAccountError(exception) && amountToCreateAccount != null) {
            Result.Failure(TapError.NoAccount(amountToCreateAccount.toString()))
        } else {
            val message = exception.localizedMessage ?: "Unknown exception during WalletManager update"
            Result.Failure(TapError.CustomError(message))
        }
    }
}