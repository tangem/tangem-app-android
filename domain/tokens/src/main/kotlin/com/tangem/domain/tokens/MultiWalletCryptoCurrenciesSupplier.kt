package com.tangem.domain.tokens

import com.tangem.domain.core.flow.FlowCachingSupplier
import com.tangem.domain.core.flow.FlowProducer
import com.tangem.domain.models.currency.CryptoCurrency
import com.tangem.domain.wallets.models.UserWalletId
import com.tangem.domain.tokens.MultiWalletCryptoCurrenciesProducer as Producer

/**
 * Producer of all crypto currencies for a multi-currency wallet with specified [UserWalletId]
 *
 * @property factory    factory for creating [Producer]
 * @property keyCreator key creator
 *
[REDACTED_AUTHOR]
 */
abstract class MultiWalletCryptoCurrenciesSupplier(
    override val factory: FlowProducer.Factory<Producer.Params, Producer>,
    override val keyCreator: (Producer.Params) -> String,
) : FlowCachingSupplier<Producer, Producer.Params, Set<CryptoCurrency>>()