package com.tangem.data.pay.flow

import arrow.core.Option
import arrow.core.some
import com.tangem.data.pay.store.PaymentAccountStatusesStore
import com.tangem.domain.core.flow.FlowProducerTools
import com.tangem.domain.models.StatusSource
import com.tangem.domain.pay.PaymentAccountStatus
import com.tangem.domain.pay.flow.PaymentAccountStatusProducer
import com.tangem.utils.coroutines.CoroutineDispatcherProvider
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.onEmpty

internal class DefaultPaymentAccountStatusProducer @AssistedInject constructor(
    @Assisted private val params: PaymentAccountStatusProducer.Params,
    override val flowProducerTools: FlowProducerTools,
    private val paymentAccountStatusesStore: PaymentAccountStatusesStore,
    private val dispatchers: CoroutineDispatcherProvider,
) : PaymentAccountStatusProducer {
    override val fallback: Option<PaymentAccountStatus>
        get() = PaymentAccountStatus.Error.Unavailable(source = StatusSource.ACTUAL).some()

    override fun produce(): Flow<PaymentAccountStatus> {
        return paymentAccountStatusesStore.get(userWalletId = params.userWalletId)
            .onEmpty { emit(value = PaymentAccountStatus.NotCreated) }
            .flowOn(dispatchers.default)
    }

    @AssistedFactory
    interface Factory : PaymentAccountStatusProducer.Factory {
        override fun create(params: PaymentAccountStatusProducer.Params): DefaultPaymentAccountStatusProducer
    }
}