package com.tangem.tap.common.analytics.converters

import com.tangem.common.Converter
import com.tangem.domain.card.CardTypeResolver
import com.tangem.tap.common.analytics.events.Basic

/**
 * Created by Anton Zhilenkov on 02.11.2022.
 */
class TopUpEventConverter : Converter<CardTypeResolver, Basic.ToppedUp?> {

    override fun convert(value: CardTypeResolver): Basic.ToppedUp? {
        val paramCardCurrency = ParamCardCurrencyConverter().convert(value) ?: return null

        return Basic.ToppedUp(paramCardCurrency)
    }
}
