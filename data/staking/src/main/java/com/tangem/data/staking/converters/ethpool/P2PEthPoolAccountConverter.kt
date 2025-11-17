package com.tangem.data.staking.converters.ethpool

import com.tangem.datasource.api.ethpool.models.response.P2PEthPoolAccountResponse
import com.tangem.datasource.api.ethpool.models.response.P2PEthPoolExitQueueDTO
import com.tangem.datasource.api.ethpool.models.response.P2PEthPoolExitRequestDTO
import com.tangem.datasource.api.ethpool.models.response.P2PEthPoolStakeDTO
import com.tangem.domain.staking.model.ethpool.*
import com.tangem.utils.converter.Converter
import org.joda.time.Instant

/**
 * Converter from P2P Account Info Response to Domain model
 */
internal object P2PEthPoolAccountConverter : Converter<P2PEthPoolAccountResponse, P2PEthPoolAccount> {

    override fun convert(value: P2PEthPoolAccountResponse): P2PEthPoolAccount {
        return P2PEthPoolAccount(
            delegatorAddress = value.delegatorAddress,
            vaultAddress = value.vaultAddress,
            stake = convertStake(value.stake),
            availableToUnstake = value.availableToUnstake,
            availableToWithdraw = value.availableToWithdraw,
            exitQueue = convertExitQueue(value.exitQueue),
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
            timestamp = Instant.ofEpochSecond(dto.timestamp),
            withdrawalTimestamp = Instant.ofEpochSecond(dto.withdrawalTimestamp),
            isClaimable = dto.isClaimable,
        )
    }
}