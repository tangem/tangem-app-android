package com.tangem.data.staking.single

import com.tangem.core.analytics.api.AnalyticsExceptionHandler
import com.tangem.core.analytics.models.ExceptionAnalyticsEvent
import com.tangem.data.staking.utils.StakingIdFactory
import com.tangem.domain.staking.model.StakingID
import com.tangem.domain.staking.model.stakekit.YieldBalance
import com.tangem.domain.staking.multi.MultiYieldBalanceProducer
import com.tangem.domain.staking.multi.MultiYieldBalanceSupplier
import com.tangem.domain.staking.single.SingleYieldBalanceProducer
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
 * Default implementation of [SingleYieldBalanceProducer]
 *
 * @property params                    params
 * @property multiYieldBalanceSupplier multi yield balance supplier
 * @property stakingIdFactory          factory for creating [StakingID]
 * @property dispatchers               dispatchers
 *
[REDACTED_AUTHOR]
 */
internal class DefaultSingleYieldBalanceProducer @AssistedInject constructor(
    @Assisted private val params: SingleYieldBalanceProducer.Params,
    private val multiYieldBalanceSupplier: MultiYieldBalanceSupplier,
    private val stakingIdFactory: StakingIdFactory,
    private val analyticsExceptionHandler: AnalyticsExceptionHandler,
    private val dispatchers: CoroutineDispatcherProvider,
) : SingleYieldBalanceProducer {

    override val fallback: YieldBalance by lazy {
        YieldBalance.Error(
            integrationId = stakingIdFactory.createIntegrationId(currencyId = params.currencyId),
            address = null,
        )
    }

    private var stakingId: StakingID? = null

    override fun produce(): Flow<YieldBalance> {
        Timber.i("Producing yield balance for params:\n$params")

        return multiYieldBalanceSupplier(
            params = MultiYieldBalanceProducer.Params(userWalletId = params.userWalletId),
        )
            .mapNotNull { balances ->
                val currentStakingId = getStakingId()

                if (currentStakingId == null) {
                    Timber.i("Staking ID is null for params: $params")
                    return@mapNotNull YieldBalance.Unsupported
                }

                val currentBalances = balances.filter { it.getStakingId() == currentStakingId }

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

                    val dataIndex = currentBalances.indexOfFirstOrNull { it is YieldBalance.Data }

                    if (dataIndex != null) {
                        currentBalances[dataIndex]
                    } else {
                        currentBalances.first()
                    }
                } else {
                    val balance = currentBalances.firstOrNull()

                    if (balance != null) {
                        Timber.i("Yield balance found for $currentStakingId:\n$balance")
                        balance
                    } else {
                        Timber.i("No yield balance found for $currentStakingId:\n${YieldBalance.Unsupported}")
                        YieldBalance.Unsupported
                    }
                }
            }
            .distinctUntilChanged()
            .flowOn(dispatchers.default)
    }

    private suspend fun getStakingId(): StakingID? {
        val saved = stakingId

        if (saved != null) return saved

        return stakingIdFactory.create(
            userWalletId = params.userWalletId,
            currencyId = params.currencyId,
            network = params.network,
        )
            .also { stakingId = it }
    }

    @AssistedFactory
    interface Factory : SingleYieldBalanceProducer.Factory {
        override fun create(params: SingleYieldBalanceProducer.Params): DefaultSingleYieldBalanceProducer
    }
}