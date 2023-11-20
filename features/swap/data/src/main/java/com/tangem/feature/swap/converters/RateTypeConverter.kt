package com.tangem.feature.swap.converters

import com.tangem.datasource.api.express.models.response.RateType
import com.tangem.utils.converter.TwoWayConverter
import com.tangem.feature.swap.domain.models.domain.RateType as RateTypeDomain

class RateTypeConverter : TwoWayConverter<RateType, RateTypeDomain> {

    override fun convert(value: RateType): RateTypeDomain {
        return when (value) {
            RateType.FIXED -> RateTypeDomain.FIXED
            RateType.FLOAT -> RateTypeDomain.FLOAT
        }
    }

    override fun convertBack(value: RateTypeDomain): RateType {
        return when (value) {
            RateTypeDomain.FIXED -> RateType.FIXED
            RateTypeDomain.FLOAT -> RateType.FLOAT
        }
    }
}