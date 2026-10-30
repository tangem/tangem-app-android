package com.tangem.data.pay.util

import com.tangem.domain.pay.model.bankofframp.BankOfframpRoute
import com.tangem.spend.datasource.pay.models.response.BankOfframpRouteResponse
import com.tangem.utils.converter.Converter

internal object BankOfframpRouteConverter : Converter<BankOfframpRouteResponse.Result, BankOfframpRoute?> {

    override fun convert(value: BankOfframpRouteResponse.Result): BankOfframpRoute? {
        return BankOfframpRoute(
            depositAddress = value.depositAddress?.takeIf(String::isNotBlank) ?: return null,
            fee = value.fee?.let(BankOfframpOptionsConverter::convertFee) ?: return null,
            eta = value.eta?.let(BankOfframpOptionsConverter::convertEta) ?: return null,
            bankAccountId = value.bankAccountId?.takeIf(String::isNotBlank) ?: return null,
        )
    }
}