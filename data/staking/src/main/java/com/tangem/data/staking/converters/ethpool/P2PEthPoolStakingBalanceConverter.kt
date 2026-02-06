package com.tangem.data.staking.converters.ethpool

import com.tangem.datasource.api.ethpool.models.response.P2PEthPoolAccountResponse
import com.tangem.domain.models.StatusSource
import com.tangem.domain.models.staking.StakingBalance
import com.tangem.domain.models.staking.StakingID
import com.tangem.domain.staking.model.StakingIntegrationID
import java.math.BigDecimal

/**
 * Converts P2PEthPool API responses to [StakingBalance].
 *
 * Uses [P2PEthPoolStakingAccountConverter] for account conversion to avoid duplication.
 */
internal object P2PEthPoolStakingBalanceConverter {

    fun convertAll(responses: Set<P2PEthPoolAccountResponse>, source: StatusSource): Set<StakingBalance> {
        return responses
            .groupBy { it.delegatorAddress }
            .map { (delegatorAddress, accountResponses) ->
                convert(delegatorAddress, accountResponses, source)
            }
            .toSet()
    }

    private fun convert(
        delegatorAddress: String,
        responses: List<P2PEthPoolAccountResponse>,
        source: StatusSource,
    ): StakingBalance {
        val stakingId = StakingID(
            integrationId = StakingIntegrationID.P2PEthPool.value,
            address = delegatorAddress,
        )

        val accounts = responses.map { P2PEthPoolStakingAccountConverter.convert(it) }

        val hasActivePosition = accounts.any { account ->
            account.stake.assets > BigDecimal.ZERO ||
                account.exitQueue.total > BigDecimal.ZERO ||
                account.availableToWithdraw > BigDecimal.ZERO
        }

        return if (hasActivePosition) {
            StakingBalance.Data.P2PEthPool(
                stakingId = stakingId,
                source = source,
                accounts = accounts,
            )
        } else {
            StakingBalance.Empty(
                stakingId = stakingId,
                source = source,
            )
        }
    }
}