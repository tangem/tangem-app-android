package com.tangem.domain.tokens.wallet

import com.tangem.domain.models.currency.CryptoCurrency
import com.tangem.domain.models.wallet.UserWallet

/**
 * Base contract for implementation of wallet's balance fetcher
 *
[REDACTED_AUTHOR]
 */
internal interface BaseWalletBalanceFetcher {

    /** Fetching sources */
    val fetchingSources: Set<FetchingSource>

    /** Get crypto currencies of [userWallet] */
    suspend fun getCryptoCurrencies(userWallet: UserWallet): Set<CryptoCurrency>
}