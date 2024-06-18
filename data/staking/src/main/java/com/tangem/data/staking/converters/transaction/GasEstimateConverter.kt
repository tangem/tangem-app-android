package com.tangem.data.staking.converters.transaction

import com.tangem.data.staking.converters.TokenConverter
import com.tangem.datasource.api.stakekit.models.response.model.transaction.GasEstimateDTO
import com.tangem.domain.staking.model.transaction.GasEstimate
import com.tangem.utils.converter.Converter

class GasEstimateConverter(
    private val tokenConverter: TokenConverter,
) : Converter<GasEstimateDTO, GasEstimate> {

    override fun convert(value: GasEstimateDTO): GasEstimate {
        return GasEstimate(
            amount = value.amount,
            token = tokenConverter.convert(value.token),
            gasLimit = value.gasLimit,
        )
    }
}
