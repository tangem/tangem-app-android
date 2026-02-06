package com.tangem.domain.account.status.producer

import arrow.core.Option
import arrow.core.none
import com.tangem.domain.account.status.supplier.SingleAccountStatusListSupplier
import com.tangem.domain.core.flow.FlowProducerTools
import com.tangem.domain.models.account.AccountStatus
import com.tangem.utils.coroutines.CoroutineDispatcherProvider
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.mapNotNull

/**
 * Default implementation of [SingleAccountStatusProducer].
 *
 * @param params Parameters required to produce the account status.
 * @param singleAccountStatusListSupplier Supplier to fetch the list of account statuses.
 * @param dispatchers Coroutine dispatcher provider for managing threading.
 */
internal class DefaultSingleAccountStatusProducer @AssistedInject constructor(
    @Assisted val params: SingleAccountStatusProducer.Params,
    override val flowProducerTools: FlowProducerTools,
    private val singleAccountStatusListSupplier: SingleAccountStatusListSupplier,
    private val dispatchers: CoroutineDispatcherProvider,
) : SingleAccountStatusProducer {

    override val fallback: Option<AccountStatus> get() = none()

    override fun produce(): Flow<AccountStatus> {
        return singleAccountStatusListSupplier(userWalletId = params.accountId.userWalletId)
            .mapNotNull { accountStatusList ->
                accountStatusList.accountStatuses.firstOrNull {
                    it.account.accountId == params.accountId
                }
            }
            .flowOn(dispatchers.default)
    }

    @AssistedFactory
    interface Factory : SingleAccountStatusProducer.Factory {
        override fun create(params: SingleAccountStatusProducer.Params): DefaultSingleAccountStatusProducer
    }
}