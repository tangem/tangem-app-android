package com.tangem.data.staking.converters.ethpool

import com.tangem.datasource.api.ethpool.models.response.P2PEthPoolAccountResponse
import com.tangem.datasource.api.ethpool.models.response.P2PEthPoolExitQueueDTO
import com.tangem.datasource.api.ethpool.models.response.P2PEthPoolExitRequestDTO
import com.tangem.datasource.api.ethpool.models.response.P2PEthPoolStakeDTO
import com.tangem.domain.models.StatusSource
import com.tangem.domain.models.staking.*
import com.tangem.domain.staking.model.StakingIntegrationID
import kotlinx.datetime.Instant

/** Converts P2PEthPool API response to [StakingBalance.Data.P2PEthPool] */
internal object P2PEthPoolStakingBalanceConverter {

    fun convert(response: P2PEthPoolAccountResponse, source: StatusSource): StakingBalance.Data.P2PEthPool {
        val stakingId = StakingID(
            integrationId = StakingIntegrationID.P2PEthPool.value,
            address = response.delegatorAddress,
        )

        val account = P2PEthPoolStakingAccount(
            delegatorAddress = response.delegatorAddress,
            vaultAddress = response.vaultAddress,
            stake = convertStake(response.stake),
            availableToUnstake = response.availableToUnstake,
            availableToWithdraw = response.availableToWithdraw,
            exitQueue = convertExitQueue(response.exitQueue),
        )

        return StakingBalance.Data.P2PEthPool(
            stakingId = stakingId,
            source = source,
            account = account,
        )
    }

    private fun convertStake(dto: P2PEthPoolStakeDTO): P2PEthPoolStake {
        return P2PEthPoolStake(
            assets = dto.assets,
            totalEarnedAssets = dto.totalEarnedAssets,
        )
    }

    private fun convertExitQueue(dto: P2PEthPoolExitQueueDTO): P2PEthPoolExitQueue {
        return P2PEthPoolExitQueue(
            total = dto.total.toBigDecimal(),
            requests = dto.requests.map(::convertExitRequest),
        )
    }

    private fun convertExitRequest(dto: P2PEthPoolExitRequestDTO): P2PEthPoolExitRequest {
        return P2PEthPoolExitRequest(
            ticket = dto.ticket,
            totalAssets = dto.totalAssets.toBigDecimal(),
            timestamp = Instant.fromEpochSeconds(dto.timestamp),
            withdrawalTimestamp = Instant.fromEpochSeconds(dto.withdrawalTimestamp),
            isClaimable = dto.isClaimable,
        )
    }
}