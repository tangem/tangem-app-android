package com.tangem.feature.swap.converters

import com.tangem.datasource.api.express.models.request.LeastTokenInfo
import com.tangem.domain.models.currency.CryptoCurrency
import com.tangem.utils.converter.Converter

class LeastTokenInfoConverter : Converter<CryptoCurrency, LeastTokenInfo> {

    override fun convert(value: CryptoCurrency): LeastTokenInfo {
        return LeastTokenInfo(
            contractAddress = (value as? CryptoCurrency.Token)?.contractAddress ?: "0",
            network = value.network.backendId,
        )
    }
}