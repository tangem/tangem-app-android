package com.tangem.data.swap.converter

import com.tangem.datasource.api.express.models.request.LeastTokenInfo
import com.tangem.domain.models.currency.CryptoCurrency
import com.tangem.domain.swap.models.TokenInfo
import com.tangem.utils.converter.Converter

class TokenInfoConverter : Converter<CryptoCurrency, LeastTokenInfo> {

    override fun convert(value: CryptoCurrency): LeastTokenInfo {
        return LeastTokenInfo(
            contractAddress = (value as? CryptoCurrency.Token)?.contractAddress ?: "0",
            network = value.network.backendId,
        )
    }

    fun convert(value: TokenInfo): LeastTokenInfo {
        return LeastTokenInfo(
            contractAddress = value.contractAddress,
            network = value.network,
        )
    }
}