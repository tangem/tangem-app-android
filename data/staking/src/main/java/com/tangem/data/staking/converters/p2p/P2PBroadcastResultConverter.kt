package com.tangem.data.staking.converters.p2p

import com.tangem.datasource.api.p2p.models.response.P2PBroadcastTransactionResponse
import com.tangem.datasource.api.p2p.models.response.P2PTransactionStatusDTO
import com.tangem.domain.staking.model.p2p.P2PBroadcastResult
import com.tangem.domain.staking.model.p2p.P2PTransactionBroadcastStatus
import com.tangem.utils.converter.Converter
import java.math.BigDecimal

/**
 * Converter from P2P Broadcast Transaction Response to Domain model
 */
internal object P2PBroadcastResultConverter : Converter<P2PBroadcastTransactionResponse, P2PBroadcastResult> {

    override fun convert(value: P2PBroadcastTransactionResponse): P2PBroadcastResult {
        return P2PBroadcastResult(
            hash = value.hash,
            status = convertStatus(value.status),
            blockNumber = value.blockNumber,
            transactionIndex = value.transactionIndex,
            gasUsed = value.gasUsed.toBigDecimalOrNull() ?: BigDecimal.ZERO,
            cumulativeGasUsed = value.cumulativeGasUsed.toBigDecimalOrNull() ?: BigDecimal.ZERO,
            effectiveGasPrice = value.effectiveGasPrice?.toBigDecimalOrNull(),
            from = value.from,
            to = value.to,
        )
    }

    private fun convertStatus(status: P2PTransactionStatusDTO): P2PTransactionBroadcastStatus {
        return when (status) {
            P2PTransactionStatusDTO.SUCCESS -> P2PTransactionBroadcastStatus.SUCCESS
            P2PTransactionStatusDTO.FAILED -> P2PTransactionBroadcastStatus.FAILED
        }
    }
}