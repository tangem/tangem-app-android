package com.tangem.domain.card.analytics

import com.tangem.blockchain.common.Blockchain
import com.tangem.core.analytics.models.AnalyticsParam.WalletType
import com.tangem.domain.card.CardTypesResolver
import com.tangem.utils.converter.Converter

class ParamCardCurrencyConverter : Converter<CardTypesResolver, WalletType?> {

    override fun convert(value: CardTypesResolver): WalletType? {
        if (value.isMultiwalletAllowed()) return WalletType.MultiCurrency

        val currency = when {
            value.isTangemNote() -> value.getBlockchain().currency
            value.isTangemTwins() -> Blockchain.Bitcoin.currency
            value.getBlockchain() != Blockchain.Unknown -> value.getBlockchain().currency
            else -> value.getPrimaryToken()?.symbol
        } ?: return null

        return WalletType.SingleCurrency(currency)
    }
}