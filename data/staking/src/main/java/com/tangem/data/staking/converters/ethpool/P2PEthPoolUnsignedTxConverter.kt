package com.tangem.data.staking.converters.ethpool

import com.tangem.datasource.api.ethpool.models.response.P2PEthPoolUnsignedTxDTO
import com.tangem.domain.staking.model.ethpool.P2PEthPoolUnsignedTx
import com.tangem.utils.converter.Converter
import java.math.BigDecimal

/**
 * Converter from P2PEthPool Unsigned Transaction DTO to Domain model
 */
internal object P2PEthPoolUnsignedTxConverter : Converter<P2PEthPoolUnsignedTxDTO, P2PEthPoolUnsignedTx> {

    override fun convert(value: P2PEthPoolUnsignedTxDTO): P2PEthPoolUnsignedTx {
        return P2PEthPoolUnsignedTx(
            serializeTx = value.serializeTx,
            to = value.to,
            data = value.data,
            value = value.value.toBigDecimalOrNull() ?: BigDecimal.ZERO,
            nonce = value.nonce,
            chainId = value.chainId,
            gasLimit = value.gasLimit,
            maxFeePerGas = value.maxFeePerGas,
            maxPriorityFeePerGas = value.maxPriorityFeePerGas,
        )
    }
}