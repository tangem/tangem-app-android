package com.tangem.data.staking.converters.transaction

import com.tangem.datasource.api.stakekit.models.response.model.transaction.StakingTransactionTypeDTO
import com.tangem.domain.staking.model.stakekit.transaction.StakingTransactionType
import com.tangem.utils.converter.Converter

@Suppress("CyclomaticComplexMethod")
class StakingTransactionTypeConverter : Converter<StakingTransactionTypeDTO, StakingTransactionType> {

    override fun convert(value: StakingTransactionTypeDTO): StakingTransactionType {
        return when (value) {
            StakingTransactionTypeDTO.SWAP -> StakingTransactionType.SWAP
            StakingTransactionTypeDTO.DEPOSIT -> StakingTransactionType.DEPOSIT
            StakingTransactionTypeDTO.APPROVAL -> StakingTransactionType.APPROVAL
            StakingTransactionTypeDTO.STAKE -> StakingTransactionType.STAKE
            StakingTransactionTypeDTO.CLAIM_UNSTAKED -> StakingTransactionType.CLAIM_UNSTAKED
            StakingTransactionTypeDTO.CLAIM_REWARDS -> StakingTransactionType.CLAIM_REWARDS
            StakingTransactionTypeDTO.RESTAKE_REWARDS -> StakingTransactionType.RESTAKE_REWARDS
            StakingTransactionTypeDTO.UNSTAKE -> StakingTransactionType.UNSTAKE
            StakingTransactionTypeDTO.SPLIT -> StakingTransactionType.SPLIT
            StakingTransactionTypeDTO.MERGE -> StakingTransactionType.MERGE
            StakingTransactionTypeDTO.LOCK -> StakingTransactionType.LOCK
            StakingTransactionTypeDTO.UNLOCK -> StakingTransactionType.UNLOCK
            StakingTransactionTypeDTO.SUPPLY -> StakingTransactionType.SUPPLY
            StakingTransactionTypeDTO.BRIDGE -> StakingTransactionType.BRIDGE
            StakingTransactionTypeDTO.VOTE -> StakingTransactionType.VOTE
            StakingTransactionTypeDTO.REVOKE -> StakingTransactionType.REVOKE
            StakingTransactionTypeDTO.RESTAKE -> StakingTransactionType.RESTAKE
            StakingTransactionTypeDTO.REBOND -> StakingTransactionType.REBOND
            StakingTransactionTypeDTO.WITHDRAW -> StakingTransactionType.WITHDRAW
            StakingTransactionTypeDTO.CREATE_ACCOUNT -> StakingTransactionType.CREATE_ACCOUNT
            StakingTransactionTypeDTO.REVEAL -> StakingTransactionType.REVEAL
            StakingTransactionTypeDTO.MIGRATE -> StakingTransactionType.MIGRATE
            StakingTransactionTypeDTO.UTXO_P_TO_C_IMPORT -> StakingTransactionType.UTXO_P_TO_C_IMPORT
            StakingTransactionTypeDTO.UTXO_C_TO_P_IMPORT -> StakingTransactionType.UTXO_C_TO_P_IMPORT
            StakingTransactionTypeDTO.UNFREEZE_LEGACY -> StakingTransactionType.UNFREEZE_LEGACY
            StakingTransactionTypeDTO.UNFREEZE_LEGACY_BANDWIDTH -> StakingTransactionType.UNFREEZE_LEGACY_BANDWIDTH
            StakingTransactionTypeDTO.UNFREEZE_LEGACY_ENERGY -> StakingTransactionType.UNFREEZE_LEGACY_ENERGY
            StakingTransactionTypeDTO.UNFREEZE_BANDWIDTH -> StakingTransactionType.UNFREEZE_BANDWIDTH
            StakingTransactionTypeDTO.UNFREEZE_ENERGY -> StakingTransactionType.UNFREEZE_ENERGY
            StakingTransactionTypeDTO.FREEZE_BANDWIDTH -> StakingTransactionType.FREEZE_BANDWIDTH
            StakingTransactionTypeDTO.FREEZE_ENERGY -> StakingTransactionType.FREEZE_ENERGY
            StakingTransactionTypeDTO.UNDELEGATE_BANDWIDTH -> StakingTransactionType.UNDELEGATE_BANDWIDTH
            StakingTransactionTypeDTO.UNDELEGATE_ENERGY -> StakingTransactionType.UNDELEGATE_ENERGY
            StakingTransactionTypeDTO.P2P_NODE_REQUEST -> StakingTransactionType.P2P_NODE_REQUEST
            StakingTransactionTypeDTO.LUGANODES_PROVISION -> StakingTransactionType.LUGANODES_PROVISION
            StakingTransactionTypeDTO.LUGANODES_EXIT_REQUEST -> StakingTransactionType.LUGANODES_EXIT_REQUEST
            StakingTransactionTypeDTO.INFSTONES_PROVISION -> StakingTransactionType.INFSTONES_PROVISION
            StakingTransactionTypeDTO.INFSTONES_EXIT_REQUEST -> StakingTransactionType.INFSTONES_EXIT_REQUEST
            StakingTransactionTypeDTO.INFSTONES_CLAIM_REQUEST -> StakingTransactionType.INFSTONES_CLAIM_REQUEST
            StakingTransactionTypeDTO.UNKNOWN -> StakingTransactionType.UNKNOWN
        }
    }
}