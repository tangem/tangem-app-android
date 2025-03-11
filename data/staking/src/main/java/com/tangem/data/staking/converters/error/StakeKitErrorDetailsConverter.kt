package com.tangem.data.staking.converters.error

import com.tangem.datasource.api.stakekit.models.response.model.error.StakeKitErrorDetailsDTO
import com.tangem.domain.staking.model.stakekit.StakingError
import com.tangem.utils.converter.Converter

internal object StakeKitErrorDetailsConverter :
    Converter<StakeKitErrorDetailsDTO, StakingError.StakeKitApiError.ErrorDetails> {

    override fun convert(value: StakeKitErrorDetailsDTO): StakingError.StakeKitApiError.ErrorDetails {
        return StakingError.StakeKitApiError.ErrorDetails(
            amount = value.amount?.toBigDecimalOrNull(),
        )
    }
}