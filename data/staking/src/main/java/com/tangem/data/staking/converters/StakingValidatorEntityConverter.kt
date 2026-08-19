package com.tangem.data.staking.converters

import com.tangem.grow.datasource.stakekit.models.response.model.YieldDTO
import com.tangem.datasource.local.txhistory.db.entity.staking.StakingValidatorEntity

/**
 * Converts a raw StakeKit [YieldDTO.ValidatorDTO] into its persisted [StakingValidatorEntity] representation.
 * Returns null when the validator has no address, since that's the entity's primary key.
 */
internal object StakingValidatorEntityConverter {

    fun convert(value: YieldDTO.ValidatorDTO): StakingValidatorEntity? {
        val address = value.address ?: return null
        return StakingValidatorEntity(
            address = address,
            status = value.status?.name,
            name = value.name,
            image = value.image,
            website = value.website,
            apr = value.apr?.toPlainString(),
            commission = value.commission,
            stakedBalance = value.stakedBalance,
            votingPower = value.votingPower,
            isPreferred = value.preferred,
        )
    }
}