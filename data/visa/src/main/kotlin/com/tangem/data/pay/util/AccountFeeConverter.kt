package com.tangem.data.pay.util

import com.tangem.domain.pay.model.AccountFee
import com.tangem.domain.pay.model.AccountFeeGroupType
import com.tangem.spend.datasource.pay.models.response.FeesResponse
import com.tangem.utils.converter.Converter

internal object AccountFeeConverter : Converter<FeesResponse.Fee, AccountFee?> {

    override fun convert(value: FeesResponse.Fee): AccountFee? {
        return AccountFee(
            type = value.type ?: return null,
            name = value.name ?: return null,
            paymentMethodId = value.paymentMethodId?.takeIf(String::isNotBlank),
            group = AccountFeeGroupType.fromString(value.group),
            amount = value.amount ?: return null,
            currency = value.currency?.takeIf(String::isNotBlank) ?: return null,
        )
    }
}