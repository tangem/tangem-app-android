package com.tangem.data.staking.converters.ethpool

import com.tangem.datasource.api.ethpool.models.response.P2PEthPoolAccountResponse
import com.tangem.datasource.api.ethpool.models.response.P2PEthPoolExitQueueDTO
import com.tangem.datasource.api.ethpool.models.response.P2PEthPoolExitRequestDTO
import com.tangem.datasource.api.ethpool.models.response.P2PEthPoolStakeDTO
import com.tangem.domain.models.staking.P2PEthPoolExitQueue
import com.tangem.domain.models.staking.P2PEthPoolExitRequest
import com.tangem.domain.models.staking.P2PEthPoolStake
import com.tangem.domain.models.staking.P2PEthPoolStakingAccount
import com.tangem.utils.converter.Converter
import kotlinx.datetime.Instant

/**
 * Converts P2PEthPool Account API response to domain [P2PEthPoolStakingAccount].
 */
internal object P2PEthPoolStakingAccountConverter : Converter<P2PEthPoolAccountResponse, P2PEthPoolStakingAccount> {

    override fun convert(value: P2PEthPoolAccountResponse): P2PEthPoolStakingAccount {
        return P2PEthPoolStakingAccount(
            delegatorAddress = value.delegatorAddress,
            vaultAddress = value.vaultAddress,
            stake = convertStake(value.stake),
            availableToUnstake = value.availableToUnstake,
            availableToWithdraw = value.availableToWithdraw,
            exitQueue = convertExitQueue(value.exitQueue),
        )
    }

    fun convertStake(dto: P2PEthPoolStakeDTO): P2PEthPoolStake {
        return P2PEthPoolStake(
            assets = dto.assets,
            totalEarnedAssets = dto.totalEarnedAssets,
        )
    }

    fun convertExitQueue(dto: P2PEthPoolExitQueueDTO): P2PEthPoolExitQueue {
        return P2PEthPoolExitQueue(
            total = dto.total,
            requests = dto.requests.map(::convertExitRequest),
        )
    }

    fun convertExitRequest(dto: P2PEthPoolExitRequestDTO): P2PEthPoolExitRequest {
        return P2PEthPoolExitRequest(
            ticket = dto.ticket,
            totalAssets = dto.totalAssets,
            timestamp = Instant.fromEpochMilliseconds(dto.timestamp),
            withdrawalTimestamp = dto.withdrawalTimestamp?.let { Instant.fromEpochMilliseconds(it) },
            isClaimable = dto.isClaimable,
        )
    }
}