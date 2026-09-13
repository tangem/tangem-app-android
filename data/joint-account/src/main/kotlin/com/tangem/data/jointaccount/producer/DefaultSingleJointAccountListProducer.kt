package com.tangem.data.jointaccount.producer

import arrow.core.Option
import arrow.core.some
import com.tangem.data.jointaccount.store.JointAccountsStore
import com.tangem.domain.core.flow.FlowProducerTools
import com.tangem.domain.jointaccount.model.JointAccount
import com.tangem.domain.jointaccount.producer.SingleJointAccountListProducer
import com.tangem.utils.coroutines.CoroutineDispatcherProvider
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map

internal class DefaultSingleJointAccountListProducer @AssistedInject constructor(
    @Assisted private val params: SingleJointAccountListProducer.Params,
    override val flowProducerTools: FlowProducerTools,
    private val store: JointAccountsStore,
    private val dispatchers: CoroutineDispatcherProvider,
) : SingleJointAccountListProducer {

    override val fallback: Option<List<JointAccount>>
        get() = emptyList<JointAccount>().some()

    override fun produce(): Flow<List<JointAccount>> {
        return store.get(userWalletId = params.userWalletId)
            .map { it.orEmpty() }
            .flowOn(dispatchers.default)
    }

    @AssistedFactory
    interface Factory : SingleJointAccountListProducer.Factory {
        override fun create(params: SingleJointAccountListProducer.Params): DefaultSingleJointAccountListProducer
    }
}