package com.tangem.domain.staking.single

import com.tangem.core.analytics.api.AnalyticsExceptionHandler
import com.tangem.core.analytics.models.ExceptionAnalyticsEvent
import com.tangem.domain.core.flow.FlowProducer
import com.tangem.domain.models.staking.StakingBalance
import com.tangem.domain.models.staking.StakingID
import com.tangem.domain.models.wallet.UserWalletId
import com.tangem.utils.extensions.indexOfFirstOrNull
import timber.log.Timber

/**
 * Producer of staking balance for selected wallet [UserWalletId]
 *
[REDACTED_AUTHOR]
 */
interface SingleStakingBalanceProducer : FlowProducer<StakingBalance> {

    data class Params(
        val userWalletId: UserWalletId,
        val stakingId: StakingID,
    ) {

        override fun toString(): String {
            return """
                SingleStakingBalanceProducer.Params(
                    userWalletId = $userWalletId,
                    stakingId = $stakingId,
                )
            """.trimIndent()
        }
    }

    interface Factory : FlowProducer.Factory<Params, SingleStakingBalanceProducer>

    companion object {

        fun selectStakingBalance(
            currentStakingId: StakingID,
            currentBalances: List<StakingBalance>,
            analyticsExceptionHandler: AnalyticsExceptionHandler,
        ): StakingBalance? {
            return if (currentBalances.size > 1) {
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
                val balance = currentBalances.firstOrNull() ?: return null

                Timber.i("Staking balance found for $currentStakingId:\n$balance")
                balance
            }
        }
    }
}