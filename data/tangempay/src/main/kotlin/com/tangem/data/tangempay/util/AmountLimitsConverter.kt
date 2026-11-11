package com.tangem.data.tangempay.util

import com.tangem.domain.tangempay.model.AmountLimits
import com.tangem.spend.datasource.tangempay.models.response.AmountLimitsResponse
import com.tangem.utils.converter.Converter

internal object AmountLimitsConverter : Converter<AmountLimitsResponse, AmountLimits?> {

    override fun convert(value: AmountLimitsResponse): AmountLimits? {
        return AmountLimits(
            min = value.min ?: return null,
            max = value.max,
            currency = value.currency?.takeIf(String::isNotBlank) ?: return null,
        )
    }
}