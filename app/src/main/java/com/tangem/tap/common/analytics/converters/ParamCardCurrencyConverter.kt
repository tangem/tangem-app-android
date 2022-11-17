package com.tangem.tap.common.analytics.converters

import com.tangem.blockchain.common.Blockchain
import com.tangem.common.Converter
import com.tangem.domain.common.SaltPayWorkaround
import com.tangem.domain.common.ScanResponse
import com.tangem.domain.common.TapWorkarounds.getTangemNoteBlockchain
import com.tangem.tap.common.analytics.events.AnalyticsParam
import com.tangem.tap.domain.extensions.isMultiwalletAllowed

/**
 * Created by Anton Zhilenkov on 02.11.2022.
 */
class ParamCardCurrencyConverter : Converter<ScanResponse, AnalyticsParam.CardCurrency?> {

    override fun convert(value: ScanResponse): AnalyticsParam.CardCurrency? {
        if (value.card.isMultiwalletAllowed) return AnalyticsParam.CardCurrency.MultiCurrency

        val type = when {
            value.isTangemNote() -> {
                value.card.getTangemNoteBlockchain()?.let { AnalyticsParam.CurrencyType.Blockchain(it) }
            }
            value.isTangemTwins() -> AnalyticsParam.CurrencyType.Blockchain(Blockchain.Bitcoin)
            value.isSaltPay() -> AnalyticsParam.CurrencyType.Token(SaltPayWorkaround.tokenFrom(Blockchain.SaltPay))
            value.getPrimaryToken() != null -> AnalyticsParam.CurrencyType.Token(value.getPrimaryToken()!!)
            else -> null
        } ?: return null

        return AnalyticsParam.CardCurrency.SingleCurrency(type)
    }
}
