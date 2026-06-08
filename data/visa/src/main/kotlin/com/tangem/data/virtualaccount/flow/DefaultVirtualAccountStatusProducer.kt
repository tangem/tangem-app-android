package com.tangem.data.virtualaccount.flow

import arrow.core.Option
import arrow.core.some
import com.tangem.data.virtualaccount.store.VirtualAccountStatusesStore
import com.tangem.domain.core.flow.FlowProducerTools
import com.tangem.domain.models.account.Account
import com.tangem.domain.models.account.AccountStatus
import com.tangem.domain.models.account.VirtualAccountStatusValue
import com.tangem.domain.virtualaccount.flow.VirtualAccountStatusProducer
import com.tangem.utils.coroutines.CoroutineDispatcherProvider
import com.tangem.utils.logging.TangemLogger
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map

internal class DefaultVirtualAccountStatusProducer @AssistedInject constructor(
    @Assisted private val params: VirtualAccountStatusProducer.Params,
    override val flowProducerTools: FlowProducerTools,
    private val virtualAccountStatusesStore: VirtualAccountStatusesStore,
    private val dispatchers: CoroutineDispatcherProvider,
) : VirtualAccountStatusProducer {

    private val logger = TangemLogger.withTag(TAG)
    private val account = Account.Virtual(userWalletId = params.userWalletId)

    override val fallback: Option<AccountStatus.Virtual>
        get() = AccountStatus.Virtual(account = account, value = VirtualAccountStatusValue.Error.Unavailable).some()

    override fun produce(): Flow<AccountStatus.Virtual> {
        return virtualAccountStatusesStore.get(userWalletId = params.userWalletId)
            .map { status ->
                if (status != null) {
                    logger.i("[${params.userWalletId}] flow emits statusType=${status.value::class.simpleName}")
                    AccountStatus.Virtual(
                        account = account,
                        value = status.value,
                    )
                } else {
                    logger.i("[${params.userWalletId}] status is null: emitting Empty fallback")
                    AccountStatus.Virtual(
                        account = account,
                        value = VirtualAccountStatusValue.Empty,
                    )
                }
            }
            .flowOn(dispatchers.default)
    }

    @AssistedFactory
    interface Factory : VirtualAccountStatusProducer.Factory {
        override fun create(params: VirtualAccountStatusProducer.Params): DefaultVirtualAccountStatusProducer
    }

    private companion object {
        private const val TAG = "VirtualAccountStatusProducer"
    }
}