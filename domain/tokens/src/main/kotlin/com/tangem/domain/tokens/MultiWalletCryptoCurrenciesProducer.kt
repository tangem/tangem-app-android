package com.tangem.domain.tokens

import com.tangem.domain.core.flow.FlowProducer
import com.tangem.domain.models.currency.CryptoCurrency
import com.tangem.domain.models.wallet.UserWalletId

/**
 * Producer of all crypto currencies for a multi-currency wallet with specified [UserWalletId]
 *
[REDACTED_AUTHOR]
 */
interface MultiWalletCryptoCurrenciesProducer : FlowProducer<Set<CryptoCurrency>> {

    data class Params(val userWalletId: UserWalletId)

    interface Factory : FlowProducer.Factory<Params, MultiWalletCryptoCurrenciesProducer>
}