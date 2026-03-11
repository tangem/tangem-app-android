package com.tangem.data.staking.single

import arrow.core.Option
import arrow.core.some
import com.tangem.core.analytics.api.AnalyticsExceptionHandler
import com.tangem.domain.core.flow.FlowProducerTools
import com.tangem.domain.models.staking.StakingBalance
import com.tangem.domain.staking.multi.MultiStakingBalanceProducer
import com.tangem.domain.staking.multi.MultiStakingBalanceSupplier
import com.tangem.domain.staking.single.SingleStakingBalanceProducer
import com.tangem.domain.staking.single.SingleStakingBalanceProducer.Companion.selectStakingBalance
import com.tangem.utils.coroutines.CoroutineDispatcherProvider
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import timber.log.Timber

/**
 * Default implementation of [SingleStakingBalanceProducer]
 *
 * @property params                       params
 * @property multiStakingBalanceSupplier  multi staking balance supplier
 * @property analyticsExceptionHandler    analytics exception handler
 * @property dispatchers                  dispatchers
 *
[REDACTED_AUTHOR]
 */
internal class DefaultSingleStakingBalanceProducer @AssistedInject constructor(
    @Assisted private val params: SingleStakingBalanceProducer.Params,
    private val multiStakingBalanceSupplier: MultiStakingBalanceSupplier,
    override val flowProducerTools: FlowProducerTools,
    private val analyticsExceptionHandler: AnalyticsExceptionHandler,
    private val dispatchers: CoroutineDispatcherProvider,
) : SingleStakingBalanceProducer {

    override val fallback: Option<StakingBalance> = StakingBalance.Error(stakingId = params.stakingId).some()

    override fun produce(): Flow<StakingBalance> {
        Timber.i("Producing staking balance for params:\n$params")

        return multiStakingBalanceSupplier(
            params = MultiStakingBalanceProducer.Params(userWalletId = params.userWalletId),
        )
            .map { balances ->
                val currentStakingId = params.stakingId

                val currentBalances = balances.filter { it.stakingId == currentStakingId }

                selectStakingBalance(
                    currentStakingId = currentStakingId,
                    currentBalances = currentBalances,
                    analyticsExceptionHandler = analyticsExceptionHandler,
                ) ?: StakingBalance.Error(stakingId = currentStakingId)
            }
            .flowOn(dispatchers.default)
    }

    @AssistedFactory
    interface Factory : SingleStakingBalanceProducer.Factory {
        override fun create(params: SingleStakingBalanceProducer.Params): DefaultSingleStakingBalanceProducer
    }
}