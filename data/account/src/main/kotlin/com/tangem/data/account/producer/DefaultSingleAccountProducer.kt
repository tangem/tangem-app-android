package com.tangem.data.account.producer

import arrow.core.Option
import arrow.core.none
import com.tangem.domain.account.producer.SingleAccountListProducer
import com.tangem.domain.account.producer.SingleAccountProducer
import com.tangem.domain.account.supplier.SingleAccountListSupplier
import com.tangem.domain.core.flow.FlowProducerTools
import com.tangem.domain.models.account.Account
import com.tangem.utils.coroutines.CoroutineDispatcherProvider
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.mapNotNull

/**
 * Default implementation of [SingleAccountProducer] that produces a flow of [Account.CryptoPortfolio]
 * for a single account identified by [SingleAccountProducer.Params.accountId].
 *
 * It uses [SingleAccountListSupplier] to get the list of accounts and filters it to find the
 * specific account.
 *
 * @property params Parameters containing the account ID for which the portfolio is produced.
 * @property singleAccountListSupplier Supplier to get the list of accounts.
 * @property dispatchers Coroutine dispatcher provider for managing threading.
 */
internal class DefaultSingleAccountProducer @AssistedInject constructor(
    @Assisted val params: SingleAccountProducer.Params,
    override val flowProducerTools: FlowProducerTools,
    private val singleAccountListSupplier: SingleAccountListSupplier,
    private val dispatchers: CoroutineDispatcherProvider,
) : SingleAccountProducer {

    override val fallback: Option<Account>
        get() = none()

    override fun produce(): Flow<Account> {
        return singleAccountListSupplier(
            params = SingleAccountListProducer.Params(userWalletId = params.accountId.userWalletId),
        )
            .mapNotNull { accountList ->
                accountList.accounts.firstOrNull {
                    it is Account.Crypto.Portfolio && params.accountId == it.accountId
                } as? Account.Crypto.Portfolio
            }
            .flowOn(dispatchers.default)
    }

    @AssistedFactory
    interface Factory : SingleAccountProducer.Factory {
        override fun create(params: SingleAccountProducer.Params): DefaultSingleAccountProducer
    }
}