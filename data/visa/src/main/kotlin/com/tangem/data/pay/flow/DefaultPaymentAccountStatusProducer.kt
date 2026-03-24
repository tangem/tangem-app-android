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

    private val account = Account.Payment(userWalletId = params.userWalletId)

    override val fallback: Option<AccountStatus.Payment>
        get() = AccountStatus.Payment(account = account, value = PaymentAccountStatusValue.Error.Unavailable).some()

    override fun produce(): Flow<AccountStatus.Payment> {
        return paymentAccountStatusesStore.get(userWalletId = params.userWalletId)
            .onEmpty { emit(value = AccountStatus.Payment(account, PaymentAccountStatusValue.NotCreated)) }
            .flowOn(dispatchers.default)
    }

    @AssistedFactory
    interface Factory : PaymentAccountStatusProducer.Factory {
        override fun create(params: PaymentAccountStatusProducer.Params): DefaultPaymentAccountStatusProducer
    }
}