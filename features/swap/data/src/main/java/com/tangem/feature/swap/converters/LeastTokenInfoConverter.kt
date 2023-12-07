package com.tangem.feature.swap.converters

import com.tangem.blockchain.common.Blockchain
import com.tangem.datasource.api.express.models.request.LeastTokenInfo
import com.tangem.domain.common.extensions.toNetworkId
import com.tangem.domain.tokens.model.CryptoCurrency
import com.tangem.utils.converter.Converter

class LeastTokenInfoConverter : Converter<CryptoCurrency, LeastTokenInfo> {

    override fun convert(value: CryptoCurrency): LeastTokenInfo {
        return LeastTokenInfo(
            contractAddress = (value as? CryptoCurrency.Token)?.contractAddress ?: "0",
            network = Blockchain.fromId(value.id.rawNetworkId).toNetworkId(),
        )
    }
}
