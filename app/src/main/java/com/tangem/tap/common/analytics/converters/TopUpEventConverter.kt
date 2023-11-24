package com.tangem.tap.common.analytics.converters

import com.tangem.common.Converter
import com.tangem.domain.common.CardTypesResolver
import com.tangem.domain.wallets.models.UserWalletId
import com.tangem.tap.common.analytics.events.Basic

/**
 * Created by Anton Zhilenkov on 02.11.2022.
 */
class TopUpEventConverter : Converter<Pair<UserWalletId, CardTypesResolver>, Basic.ToppedUp?> {

    override fun convert(value: Pair<UserWalletId, CardTypesResolver>): Basic.ToppedUp? {
        val (userWalletId, resolver) = value
        val paramCardCurrency = ParamCardCurrencyConverter().convert(resolver) ?: return null

        return Basic.ToppedUp(userWalletId, paramCardCurrency)
    }
}
