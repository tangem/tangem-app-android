package com.tangem.data.staking.converters.transaction

import com.tangem.datasource.api.stakekit.models.response.model.transaction.StakingGasEstimateDTO
import com.tangem.datasource.local.token.converter.YieldTokenConverter
import com.tangem.domain.staking.model.stakekit.transaction.StakingGasEstimate
import com.tangem.utils.converter.Converter

internal object GasEstimateConverter : Converter<StakingGasEstimateDTO, StakingGasEstimate> {

    override fun convert(value: StakingGasEstimateDTO): StakingGasEstimate {
        return StakingGasEstimate(
            amount = value.amount,
            token = YieldTokenConverter.convert(value.token),
            gasLimit = value.gasLimit,
        )
    }
}