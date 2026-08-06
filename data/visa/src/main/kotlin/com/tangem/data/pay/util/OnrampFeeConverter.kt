package com.tangem.data.pay.util

import com.tangem.domain.models.account.TangemPayOnrampFee
import com.tangem.spend.datasource.pay.models.response.FeesResponse

internal object OnrampFeeConverter {

    fun convertList(values: List<FeesResponse.Fee>): List<TangemPayOnrampFee> = values.mapNotNull(::convertOrNull)

    private fun convertOrNull(value: FeesResponse.Fee): TangemPayOnrampFee? {
        return TangemPayOnrampFee(
            type = value.type ?: return null,
            name = value.name ?: return null,
            amount = value.amount ?: return null,
            currency = value.currency ?: return null,
        )
    }
}