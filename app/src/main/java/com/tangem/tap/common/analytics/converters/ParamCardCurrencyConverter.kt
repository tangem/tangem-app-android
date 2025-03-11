package com.tangem.tap.common.analytics.converters

import com.tangem.blockchain.common.Blockchain
import com.tangem.domain.common.CardTypesResolver
import com.tangem.tap.common.analytics.events.AnalyticsParam
import com.tangem.utils.converter.Converter
import com.tangem.core.analytics.models.AnalyticsParam as CoreAnalyticsParam

/**
[REDACTED_AUTHOR]
 */
class ParamCardCurrencyConverter : Converter<CardTypesResolver, CoreAnalyticsParam.WalletType?> {

    override fun convert(value: CardTypesResolver): CoreAnalyticsParam.WalletType? {
        if (value.isMultiwalletAllowed()) return CoreAnalyticsParam.WalletType.MultiCurrency

        val type = when {
            value.isTangemNote() -> AnalyticsParam.CurrencyType.Blockchain(value.getBlockchain())
            value.isTangemTwins() -> AnalyticsParam.CurrencyType.Blockchain(Blockchain.Bitcoin)
            value.getBlockchain() != Blockchain.Unknown -> AnalyticsParam.CurrencyType.Blockchain(value.getBlockchain())
            value.getPrimaryToken() != null -> AnalyticsParam.CurrencyType.Token(value.getPrimaryToken()!!)
            else -> null
        } ?: return null

        return CoreAnalyticsParam.WalletType.SingleCurrency(type.value)
    }
}