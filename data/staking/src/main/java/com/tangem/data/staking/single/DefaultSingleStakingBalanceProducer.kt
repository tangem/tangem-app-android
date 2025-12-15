package com.tangem.data.staking.single

import arrow.core.Option
import arrow.core.some
import com.tangem.core.analytics.api.AnalyticsExceptionHandler
import com.tangem.core.analytics.models.ExceptionAnalyticsEvent
import com.tangem.domain.models.staking.StakingBalance
import com.tangem.domain.staking.multi.MultiStakingBalanceProducer
import com.tangem.domain.staking.multi.MultiStakingBalanceSupplier
import com.tangem.domain.staking.single.SingleStakingBalanceProducer
import com.tangem.utils.coroutines.CoroutineDispatcherProvider
import com.tangem.utils.extensions.indexOfFirstOrNull
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.mapNotNull
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
    private val analyticsExceptionHandler: AnalyticsExceptionHandler,
    private val dispatchers: CoroutineDispatcherProvider,
) : SingleStakingBalanceProducer {

    override val fallback: Option<StakingBalance> = StakingBalance.Error(stakingId = params.stakingId).some()

    override fun produce(): Flow<StakingBalance> {
        Timber.i("Producing staking balance for params:\n$params")

        return multiStakingBalanceSupplier(
            params = MultiStakingBalanceProducer.Params(userWalletId = params.userWalletId),
        )
            .mapNotNull { balances ->
                val currentStakingId = params.stakingId

                val currentBalances = balances.filter { it.stakingId == currentStakingId }

                if (currentBalances.size > 1) {
                    analyticsExceptionHandler.sendException(
                        event = ExceptionAnalyticsEvent(
                            exception = IllegalStateException("Multiple balances found for staking ID"),
                            params = mapOf(
                                "stakingId" to currentStakingId.toString(),
                                "balances" to currentBalances.joinToString(",") { it.toString() },
                            ),
                        ),
                    )

                    Timber.e(
                        "Multiple balances found for staking ID $currentStakingId:\n%s",
                        currentBalances.joinToString("\n"),
                    )

                    val dataIndex = currentBalances.indexOfFirstOrNull { it is StakingBalance.Data }

                    if (dataIndex != null) {
                        currentBalances[dataIndex]
                    } else {
                        currentBalances.first()
                    }
                } else {
                    val balance = currentBalances.firstOrNull() ?: return@mapNotNull null

                    Timber.i("Staking balance found for $currentStakingId:\n$balance")
                    balance
                }
            }
            .distinctUntilChanged()
            .flowOn(dispatchers.default)
    }

    @AssistedFactory
    interface Factory : SingleStakingBalanceProducer.Factory {
        override fun create(params: SingleStakingBalanceProducer.Params): DefaultSingleStakingBalanceProducer
    }
}