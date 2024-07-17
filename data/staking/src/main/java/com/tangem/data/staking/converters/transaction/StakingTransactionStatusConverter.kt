package com.tangem.data.staking.converters.transaction

import com.tangem.datasource.api.stakekit.models.response.model.transaction.StakingTransactionStatusDTO
import com.tangem.domain.staking.model.stakekit.transaction.StakingTransactionStatus
import com.tangem.utils.converter.Converter

class StakingTransactionStatusConverter : Converter<StakingTransactionStatusDTO, StakingTransactionStatus> {

    override fun convert(value: StakingTransactionStatusDTO): StakingTransactionStatus {
        return when (value) {
            StakingTransactionStatusDTO.NOT_FOUND -> StakingTransactionStatus.NOT_FOUND
            StakingTransactionStatusDTO.CREATED -> StakingTransactionStatus.CREATED
            StakingTransactionStatusDTO.BLOCKED -> StakingTransactionStatus.BLOCKED
            StakingTransactionStatusDTO.WAITING_FOR_SIGNATURE -> StakingTransactionStatus.WAITING_FOR_SIGNATURE
            StakingTransactionStatusDTO.SIGNED -> StakingTransactionStatus.SIGNED
            StakingTransactionStatusDTO.BROADCASTED -> StakingTransactionStatus.BROADCASTED
            StakingTransactionStatusDTO.PENDING -> StakingTransactionStatus.PENDING
            StakingTransactionStatusDTO.CONFIRMED -> StakingTransactionStatus.CONFIRMED
            StakingTransactionStatusDTO.FAILED -> StakingTransactionStatus.FAILED
            StakingTransactionStatusDTO.SKIPPED -> StakingTransactionStatus.SKIPPED
            StakingTransactionStatusDTO.UNKNOWN -> StakingTransactionStatus.UNKNOWN
        }
    }
}
