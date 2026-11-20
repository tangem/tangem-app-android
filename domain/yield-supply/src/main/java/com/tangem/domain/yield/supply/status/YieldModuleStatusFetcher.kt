package com.tangem.domain.yield.supply.status

import com.tangem.domain.core.flow.FlowFetcher
import com.tangem.domain.models.currency.CryptoCurrency
import com.tangem.domain.models.wallet.UserWalletId

/** Fetches the Yield Module statuses of [Params.tokens] and keeps them for [YieldModuleStatusSupplier] */
interface YieldModuleStatusFetcher : FlowFetcher<YieldModuleStatusFetcher.Params> {

    data class Params(
        val userWalletId: UserWalletId,
        val tokens: Set<CryptoCurrency.Token>,
    ) {

        override fun toString(): String {
            return """
                YieldModuleStatusFetcher.Params(
                    userWalletId = $userWalletId,
                    tokens: ${tokens.joinToString { it.id.value }}
                )
            """.trimIndent()
        }
    }
}