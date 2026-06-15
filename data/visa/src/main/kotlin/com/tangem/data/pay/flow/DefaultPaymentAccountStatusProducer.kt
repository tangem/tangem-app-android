package com.tangem.data.pay.flow

import arrow.core.Option
import arrow.core.some
import com.tangem.data.pay.store.PaymentAccountStatusesStore
import com.tangem.domain.core.flow.FlowProducerTools
import com.tangem.domain.models.account.Account
import com.tangem.domain.models.account.AccountStatus
import com.tangem.domain.models.account.PaymentAccountStatusValue
import com.tangem.domain.pay.flow.PaymentAccountStatusProducer
import com.tangem.utils.coroutines.CoroutineDispatcherProvider
import com.tangem.utils.logging.TangemLogger
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onStart

private const val TAG = "PaymentAccountStatusProducer"

internal class DefaultPaymentAccountStatusProducer @AssistedInject constructor(
    @Assisted private val params: PaymentAccountStatusProducer.Params,
    override val flowProducerTools: FlowProducerTools,
    private val paymentAccountStatusesStore: PaymentAccountStatusesStore,
    private val dispatchers: CoroutineDispatcherProvider,
) : PaymentAccountStatusProducer {

    private val account = Account.Payment(userWalletId = params.userWalletId)
    private val logger = TangemLogger.withTag(TAG)

    override val fallback: Option<AccountStatus.Payment>
        get() = AccountStatus.Payment(account = account, value = PaymentAccountStatusValue.Error.Unavailable).some()

    override fun produce(): Flow<AccountStatus.Payment> {
        logger.i("[${params.userWalletId}] produce() called")
        return paymentAccountStatusesStore.get(userWalletId = params.userWalletId)
            .onStart { logger.i("[${params.userWalletId}] flow subscribed to store") }
            .map { status ->
                if (status != null) {
                    logger.i("[${params.userWalletId}] flow emits statusType=${status.value::class.simpleName}")
                    AccountStatus.Payment(
                        account = account,
                        value = status.value,
                    )
                } else {
                    logger.i("[${params.userWalletId}] status is null: emitting Empty fallback")
                    AccountStatus.Payment(
                        account = account,
                        value = PaymentAccountStatusValue.Empty,
                    )
                }
            }
            .flowOn(dispatchers.default)
    }

    @AssistedFactory
    interface Factory : PaymentAccountStatusProducer.Factory {
        override fun create(params: PaymentAccountStatusProducer.Params): DefaultPaymentAccountStatusProducer
    }
}