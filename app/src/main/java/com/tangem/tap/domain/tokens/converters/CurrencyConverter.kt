package com.tangem.tap.domain.tokens.converters

import com.tangem.datasource.api.tangemTech.models.TokenBody
import com.tangem.domain.common.extensions.toNetworkId
import com.tangem.tap.features.wallet.models.Currency
import com.tangem.utils.converter.Converter

/** Converter from domain model [Currency] to data model [TokenBody] */
object CurrencyConverter : Converter<Currency, TokenBody> {

    override fun convert(value: Currency) = TokenBody(
        id = value.coinId,
        networkId = value.blockchain.toNetworkId(),
        derivationPath = value.derivationPath,
        name = value.currencyName,
        symbol = value.currencySymbol,
        decimals = value.decimals,
        contractAddress = if (value is Currency.Token) value.token.contractAddress else null
    )
}
