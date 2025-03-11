package com.tangem.data.tokens.converters

import com.tangem.domain.tokens.model.blockchains.UtxoAmountLimit
import com.tangem.utils.converter.Converter
import com.tangem.blockchain.common.UtxoAmountLimit as BlockchainUtxoAmountLimit

internal class UtxoConverter : Converter<BlockchainUtxoAmountLimit, UtxoAmountLimit> {
    override fun convert(value: BlockchainUtxoAmountLimit): UtxoAmountLimit {
        return UtxoAmountLimit(
            limit = value.limit,
            availableToSpend = value.availableToSpend,
            availableToSend = value.availableToSend,
        )
    }
}