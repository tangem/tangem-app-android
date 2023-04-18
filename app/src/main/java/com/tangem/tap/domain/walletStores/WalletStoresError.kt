package com.tangem.tap.domain.walletStores

import com.tangem.blockchain.common.Blockchain
import com.tangem.common.core.TangemError

sealed class WalletStoresError(code: Int) : TangemError(code) {
    override val silent: Boolean
        get() = (cause as? TangemError)?.silent == true

    override val messageResId: Int? = null
    override val message: String?
        get() = customMessage

    class FetchFiatRatesError(
        currencies: List<String>,
        override val cause: Throwable?,
    ) : WalletStoresError(code = 60011) {
        override var customMessage: String = "Failed to fetch fiat rates for currencies $currencies"
    }

    class UnknownBlockchain : WalletStoresError(code = 60012) {
        override var customMessage: String = "Unknown blockchain"
    }

    object NoInternetConnection : WalletStoresError(code = 60013) {
        override var customMessage: String = "No internet connection"
    }

    class WalletManagerNotCreated(blockchain: Blockchain) : WalletStoresError(code = 60014) {
        override var customMessage: String = "Wallet manager can not be created for $blockchain"
    }

    class UpdateWalletManagerTokensError(
        blockchain: Blockchain,
        override val cause: Throwable,
    ) : WalletStoresError(code = 600015) {
        override var customMessage: String = "Unable to update wallet manager tokens for currency $blockchain: $cause"
    }
}
