package com.tangem.data.staking.converters

import com.tangem.datasource.api.stakekit.models.response.model.BalanceDTO.BalanceTypeDTO
import com.tangem.domain.staking.model.stakekit.BalanceType
import com.tangem.utils.converter.Converter

internal class BalanceTypeConverter : Converter<BalanceTypeDTO, BalanceType> {

    override fun convert(value: BalanceTypeDTO): BalanceType {
        return when (value) {
            BalanceTypeDTO.AVAILABLE -> BalanceType.AVAILABLE
            BalanceTypeDTO.STAKED -> BalanceType.STAKED
            BalanceTypeDTO.PREPARING -> BalanceType.PREPARING
            BalanceTypeDTO.LOCKED -> BalanceType.LOCKED
            BalanceTypeDTO.UNSTAKING -> BalanceType.UNSTAKING
            BalanceTypeDTO.UNLOCKING -> BalanceType.UNLOCKING
            BalanceTypeDTO.UNSTAKED -> BalanceType.UNSTAKED
            BalanceTypeDTO.REWARDS -> BalanceType.REWARDS
            BalanceTypeDTO.UNKNOWN -> BalanceType.UNKNOWN
        }
    }
}
