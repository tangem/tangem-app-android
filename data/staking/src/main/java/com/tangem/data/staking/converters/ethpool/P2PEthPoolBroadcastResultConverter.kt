package com.tangem.data.staking.converters.ethpool

import com.tangem.datasource.api.ethpool.models.response.P2PEthPoolBroadcastResponse
import com.tangem.domain.staking.model.ethpool.P2PEthPoolBroadcastResult
import com.tangem.utils.converter.Converter

/**
 * Converter from P2PEthPool Broadcast Transaction Response to Domain model
 */
internal object P2PEthPoolBroadcastResultConverter : Converter<P2PEthPoolBroadcastResponse, P2PEthPoolBroadcastResult> {

    override fun convert(value: P2PEthPoolBroadcastResponse): P2PEthPoolBroadcastResult {
        return P2PEthPoolBroadcastResult(
            hash = value.hash,
            status = value.status,
            blockNumber = value.blockNumber,
            transactionIndex = value.transactionIndex,
            gasUsed = value.gasUsed?.toBigDecimalOrNull(),
            cumulativeGasUsed = value.cumulativeGasUsed?.toBigDecimalOrNull(),
            effectiveGasPrice = value.effectiveGasPrice?.toBigDecimalOrNull(),
            from = value.from,
            to = value.to,
        )
    }
}