package com.tangem.domain.networks.single

import com.tangem.domain.core.flow.FlowFetcher
import com.tangem.domain.tokens.model.CryptoCurrency
import com.tangem.domain.tokens.model.Network
import com.tangem.domain.wallets.models.UserWalletId

/**
 * Fetcher of network status [Network] for wallet with [UserWalletId]
 *
[REDACTED_AUTHOR]
 */
interface SingleNetworkStatusFetcher : FlowFetcher<SingleNetworkStatusFetcher.Params> {

    /** Params */
    sealed interface Params {

        val userWalletId: UserWalletId
        val network: Network

        data class Simple(override val userWalletId: UserWalletId, override val network: Network) : Params

        data class Prepared(
            override val userWalletId: UserWalletId,
            override val network: Network,
            val addedNetworkCurrencies: Set<CryptoCurrency>,
        ) : Params
    }
}