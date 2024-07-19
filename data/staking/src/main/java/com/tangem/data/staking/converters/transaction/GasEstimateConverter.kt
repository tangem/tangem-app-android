package com.tangem.data.staking.converters.transaction

import com.tangem.data.staking.converters.TokenConverter
import com.tangem.datasource.api.stakekit.models.response.model.transaction.StakingGasEstimateDTO
import com.tangem.domain.staking.model.stakekit.transaction.StakingGasEstimate
import com.tangem.utils.converter.Converter

class GasEstimateConverter(
    private val tokenConverter: TokenConverter,
) : Converter<StakingGasEstimateDTO, StakingGasEstimate> {

    override fun convert(value: StakingGasEstimateDTO): StakingGasEstimate {
        return StakingGasEstimate(
            amount = value.amount,
            token = tokenConverter.convert(value.token),
            gasLimit = value.gasLimit,
        )
    }
}
