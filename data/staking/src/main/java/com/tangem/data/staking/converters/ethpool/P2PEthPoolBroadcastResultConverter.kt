package com.tangem.data.staking.converters.ethpool

import com.tangem.datasource.api.ethpool.models.response.P2PEthPoolBroadcastResponse
import com.tangem.datasource.api.ethpool.models.response.P2PEthPoolTxStatusDTO
import com.tangem.domain.staking.model.ethpool.P2PEthPoolBroadcastResult
import com.tangem.domain.staking.model.ethpool.P2PEthPoolBroadcastStatus
import com.tangem.utils.converter.Converter
import java.math.BigDecimal

/**
 * Converter from P2PEthPool Broadcast Transaction Response to Domain model
 */
internal object P2PEthPoolBroadcastResultConverter : Converter<P2PEthPoolBroadcastResponse, P2PEthPoolBroadcastResult> {

    override fun convert(value: P2PEthPoolBroadcastResponse): P2PEthPoolBroadcastResult {
        return P2PEthPoolBroadcastResult(
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

    private fun convertStatus(status: P2PEthPoolTxStatusDTO): P2PEthPoolBroadcastStatus {
        return when (status) {
            P2PEthPoolTxStatusDTO.SUCCESS -> P2PEthPoolBroadcastStatus.SUCCESS
            P2PEthPoolTxStatusDTO.FAILED -> P2PEthPoolBroadcastStatus.FAILED
        }
    }
}