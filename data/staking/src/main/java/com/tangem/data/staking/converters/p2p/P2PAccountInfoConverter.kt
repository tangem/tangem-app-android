package com.tangem.data.staking.converters.p2p

import com.tangem.datasource.api.p2p.models.response.P2PAccountInfoResponse
import com.tangem.datasource.api.p2p.models.response.P2PExitQueueDTO
import com.tangem.datasource.api.p2p.models.response.P2PExitRequestDTO
import com.tangem.datasource.api.p2p.models.response.P2PStakeDTO
import com.tangem.domain.staking.model.p2p.*
import com.tangem.utils.converter.Converter
import org.joda.time.Instant

/**
 * Converter from P2P Account Info Response to Domain model
 */
internal object P2PAccountInfoConverter : Converter<P2PAccountInfoResponse, P2PAccountInfo> {

    override fun convert(value: P2PAccountInfoResponse): P2PAccountInfo {
        return P2PAccountInfo(
            delegatorAddress = value.delegatorAddress,
            vaultAddress = value.vaultAddress,
            stake = convertStake(value.stake),
            availableToUnstake = value.availableToUnstake,
            availableToWithdraw = value.availableToWithdraw,
            exitQueue = convertExitQueue(value.exitQueue),
        )
    }

    private fun convertStake(dto: P2PStakeDTO): P2PStake {
        return P2PStake(
            assets = dto.assets,
            totalEarnedAssets = dto.totalEarnedAssets,
        )
    }

    private fun convertExitQueue(dto: P2PExitQueueDTO): P2PExitQueue {
        return P2PExitQueue(
            total = dto.total.toBigDecimal(),
            requests = dto.requests.map(::convertExitRequest),
        )
    }

    private fun convertExitRequest(dto: P2PExitRequestDTO): P2PExitRequest {
        return P2PExitRequest(
            ticket = dto.ticket,
            totalAssets = dto.totalAssets.toBigDecimal(),
            timestamp = Instant.ofEpochSecond(dto.timestamp),
            withdrawalTimestamp = Instant.ofEpochSecond(dto.withdrawalTimestamp),
            isClaimable = dto.isClaimable,
        )
    }
}