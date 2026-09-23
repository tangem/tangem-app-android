package com.tangem.data.moneyaccount.flow

import arrow.core.Option
import arrow.core.some
import com.tangem.data.moneyaccount.store.MoneyAccountStatusesStore
import com.tangem.domain.core.flow.FlowProducerTools
import com.tangem.domain.models.account.Account
import com.tangem.domain.models.account.AccountStatus
import com.tangem.domain.models.account.MoneyAccountStatusValue
import com.tangem.domain.moneyaccount.flow.MoneyAccountStatusProducer
import com.tangem.utils.coroutines.CoroutineDispatcherProvider
import com.tangem.utils.logging.TangemLogger
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map

internal class DefaultMoneyAccountStatusProducer @AssistedInject constructor(
    @Assisted private val params: MoneyAccountStatusProducer.Params,
    override val flowProducerTools: FlowProducerTools,
    private val moneyAccountStatusesStore: MoneyAccountStatusesStore,
    private val dispatchers: CoroutineDispatcherProvider,
) : MoneyAccountStatusProducer {

    private val logger = TangemLogger.withTag(TAG)
    private val account = Account.Money(userWalletId = params.userWalletId)

    override val fallback: Option<AccountStatus.Money>
        get() = AccountStatus.Money(account = account, value = MoneyAccountStatusValue.Error.Unavailable).some()

    override fun produce(): Flow<AccountStatus.Money> {
        return moneyAccountStatusesStore.get(userWalletId = params.userWalletId)
            .map { status ->
                if (status != null) {
                    logger.i("[${params.userWalletId}] flow emits statusType=${status.value::class.simpleName}")
                    AccountStatus.Money(
                        account = account,
                        value = status.value,
                    )
                } else {
                    logger.i("[${params.userWalletId}] status is null: emitting Empty fallback")
                    AccountStatus.Money(
                        account = account,
                        value = MoneyAccountStatusValue.Empty,
                    )
                }
            }
            .flowOn(dispatchers.default)
    }

    @AssistedFactory
    interface Factory : MoneyAccountStatusProducer.Factory {
        override fun create(params: MoneyAccountStatusProducer.Params): DefaultMoneyAccountStatusProducer
    }

    private companion object {
        private const val TAG = "MoneyAccountStatusProducer"
    }
}