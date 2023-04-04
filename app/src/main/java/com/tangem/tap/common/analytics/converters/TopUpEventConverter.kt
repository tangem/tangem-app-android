package com.tangem.tap.common.analytics.converters

import com.tangem.common.Converter
import com.tangem.domain.common.CardTypesResolver
import com.tangem.tap.common.analytics.events.Basic

/**
[REDACTED_AUTHOR]
 */
class TopUpEventConverter : Converter<CardTypesResolver, Basic.ToppedUp?> {

    override fun convert(value: CardTypesResolver): Basic.ToppedUp? {
        val paramCardCurrency = ParamCardCurrencyConverter().convert(value) ?: return null

        return Basic.ToppedUp(paramCardCurrency)
    }
}