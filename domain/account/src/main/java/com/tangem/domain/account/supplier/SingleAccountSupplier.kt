package com.tangem.domain.account.supplier

import com.tangem.domain.account.producer.SingleAccountProducer
import com.tangem.domain.core.flow.FlowCachingSupplier
import com.tangem.domain.models.account.Account
import com.tangem.domain.models.account.AccountId
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.filterIsInstance

/**
 * Supplies instances of [SingleAccountProducer] that produce flows of [Account]
 * for individual accounts. Each producer is uniquely identified by its [SingleAccountProducer.Params].
 * Use [filterCryptoPortfolioAccount], [filterJointAccount] or [filterPortfolioAccount] to narrow
 * the flow to a specific [Account] subtype.
 *
 * @property factory A factory to create instances of [SingleAccountProducer].
 * @property keyCreator A function that generates a unique key for caching based on [SingleAccountProducer.Params].
 */
abstract class SingleAccountSupplier(
    override val factory: SingleAccountProducer.Factory,
    override val keyCreator: (SingleAccountProducer.Params) -> String,
) : FlowCachingSupplier<SingleAccountProducer, SingleAccountProducer.Params, Account>() {

    operator fun invoke(accountId: AccountId): Flow<Account> {
        return invoke(params = SingleAccountProducer.Params(accountId))
    }

    /** Convenience filter for callers that only ever handle [Account.CryptoPortfolio]. */
    fun filterCryptoPortfolioAccount(accountId: AccountId): Flow<Account.CryptoPortfolio> {
        return invoke(accountId).filterIsInstance()
    }

    /** Convenience filter for callers that only ever handle [Account.Portfolio]. */
    fun filterPortfolioAccount(accountId: AccountId): Flow<Account.Portfolio> {
        return invoke(accountId).filterIsInstance()
    }
}