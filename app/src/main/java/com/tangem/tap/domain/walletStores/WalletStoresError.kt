package com.tangem.tap.domain.walletStores

import com.tangem.blockchain.common.Blockchain
import com.tangem.common.core.TangemError

sealed class WalletStoresError(code: Int) : TangemError(code) {
    override val silent: Boolean
        get() = (cause as? TangemError)?.silent == true

    override val messageResId: Int? = null
    override val message: String?
        get() = customMessage

    @Suppress("MagicNumber")
    class FetchFiatRatesError(
        currencies: List<String>,
        override val cause: Throwable?,
    ) : WalletStoresError(60011) {
        override var customMessage: String = "Failed to fetch fiat rates for currencies $currencies"
    }

    @Suppress("MagicNumber")
    class UnknownBlockchain : WalletStoresError(60012) {
        override var customMessage: String = "Unknown blockchain"
    }

    @Suppress("MagicNumber")
    object NoInternetConnection : WalletStoresError(60013) {
        override var customMessage: String = "No internet connection"
    }

    @Suppress("MagicNumber")
    class WalletManagerNotCreated(blockchain: Blockchain) : WalletStoresError(60014) {
        override var customMessage: String = "Wallet manager can not be created for $blockchain"
    }

    @Suppress("MagicNumber")
    class UpdateWalletManagerError(
        blockchain: Blockchain,
        override val cause: Throwable,
    ) : WalletStoresError(600015) {
        override var customMessage: String = "Unable to update wallet manager for currency $blockchain: $cause"
    }
}
