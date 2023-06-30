package com.tangem.tap.common.analytics.converters

import com.tangem.blockchain.common.Blockchain
import com.tangem.common.Converter
import com.tangem.domain.card.CardTypeResolver
import com.tangem.tap.common.analytics.events.AnalyticsParam

/**
[REDACTED_AUTHOR]
 */
class ParamCardCurrencyConverter : Converter<CardTypeResolver, AnalyticsParam.CardCurrency?> {

    override fun convert(value: CardTypeResolver): AnalyticsParam.CardCurrency? {
        if (value.isMultiwalletAllowed()) return AnalyticsParam.CardCurrency.MultiCurrency

        val type = when {
            value.isTangemNote() -> AnalyticsParam.CurrencyType.Blockchain(value.getBlockchain())
            value.isTangemTwins() -> AnalyticsParam.CurrencyType.Blockchain(Blockchain.Bitcoin)
            value.getBlockchain() != Blockchain.Unknown -> AnalyticsParam.CurrencyType.Blockchain(value.getBlockchain())
            value.getPrimaryToken() != null -> AnalyticsParam.CurrencyType.Token(value.getPrimaryToken()!!)
            else -> null
        } ?: return null

        return AnalyticsParam.CardCurrency.SingleCurrency(type)
    }
}