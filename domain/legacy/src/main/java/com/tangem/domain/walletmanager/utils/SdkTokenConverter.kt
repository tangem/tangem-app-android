package com.tangem.domain.walletmanager.utils

import com.tangem.domain.tokens.model.Token
import com.tangem.utils.converter.Converter
import com.tangem.blockchain.common.Token as SdkToken
import com.tangem.domain.tokens.model.Token as DomainToken

internal class SdkTokenConverter : Converter<DomainToken, SdkToken?> {

    override fun convert(value: DomainToken): SdkToken? {
        return value.contractAddress?.let { contractAddress ->
            SdkToken(
                id = value.id.value.takeUnless { value.isCustom },
                name = value.name,
                symbol = value.symbol,
                contractAddress = contractAddress,
                decimals = value.decimals,
            )
        }
    }

    override fun convertList(input: List<Token>): List<SdkToken> {
        return input.mapNotNull(::convert)
    }
}
