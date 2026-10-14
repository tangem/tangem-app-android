package com.tangem.data.swap.converter

import com.tangem.domain.express.models.ExpressRateType
import com.tangem.grow.datasource.express.models.response.RateType
import com.tangem.utils.converter.Converter

internal class ExpressRateTypeConverter : Converter<RateType, ExpressRateType> {

    override fun convert(value: RateType): ExpressRateType = when (value) {
        RateType.FLOAT -> ExpressRateType.Float
        RateType.FIXED -> ExpressRateType.Fixed
    }
}