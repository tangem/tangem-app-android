package com.tangem.domain.account.supplier

import com.tangem.domain.account.producer.SingleAccountProducer
import com.tangem.domain.core.flow.FlowCachingSupplier
import com.tangem.domain.models.account.Account

/**
 * Supplies instances of [SingleAccountProducer] that produce flows of [Account]
 * for individual accounts. Each producer is uniquely identified by its [SingleAccountProducer.Params].
 *
 * @property factory A factory to create instances of [SingleAccountProducer].
 * @property keyCreator A function that generates a unique key for caching based on [SingleAccountProducer.Params].
 */
abstract class SingleAccountSupplier(
    override val factory: SingleAccountProducer.Factory,
    override val keyCreator: (SingleAccountProducer.Params) -> String,
) : FlowCachingSupplier<SingleAccountProducer, SingleAccountProducer.Params, Account>()