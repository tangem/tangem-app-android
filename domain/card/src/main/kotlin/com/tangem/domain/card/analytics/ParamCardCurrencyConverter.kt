package com.tangem.domain.card.analytics

import com.tangem.blockchain.common.Blockchain
import com.tangem.core.analytics.models.AnalyticsParam.WalletType
import com.tangem.domain.card.CardTypesResolver
import com.tangem.utils.converter.Converter

class ParamCardCurrencyConverter : Converter<CardTypesResolver, WalletType?> {

    override fun convert(value: CardTypesResolver): WalletType? {
        if (value.isMultiwalletAllowed()) return WalletType.MultiCurrency

        val type = when {
            value.isTangemNote() -> AnalyticsParam.CurrencyType.Blockchain(value.getBlockchain())
            value.isTangemTwins() -> AnalyticsParam.CurrencyType.Blockchain(Blockchain.Bitcoin)
            value.getBlockchain() != Blockchain.Unknown -> AnalyticsParam.CurrencyType.Blockchain(value.getBlockchain())
            value.getPrimaryToken() != null -> AnalyticsParam.CurrencyType.Token(value.getPrimaryToken()!!)
            else -> null
        } ?: return null

        return WalletType.SingleCurrency(type.value)
    }
}