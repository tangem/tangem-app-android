package com.tangem.data.staking.converters.p2p

import com.tangem.datasource.api.p2p.models.response.P2PUnsignedTransactionDTO
import com.tangem.domain.staking.model.p2p.P2PUnsignedTransaction
import com.tangem.utils.converter.Converter
import java.math.BigDecimal

/**
 * Converter from P2P Unsigned Transaction DTO to Domain model
 */
internal object P2PUnsignedTransactionConverter : Converter<P2PUnsignedTransactionDTO, P2PUnsignedTransaction> {

    override fun convert(value: P2PUnsignedTransactionDTO): P2PUnsignedTransaction {
        return P2PUnsignedTransaction(
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