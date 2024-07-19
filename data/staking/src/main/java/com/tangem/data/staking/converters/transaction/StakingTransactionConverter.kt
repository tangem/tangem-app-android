package com.tangem.data.staking.converters.transaction

import com.tangem.data.staking.converters.StakingNetworkTypeConverter
import com.tangem.datasource.api.stakekit.models.response.model.transaction.StakingTransactionDTO
import com.tangem.domain.staking.model.stakekit.transaction.StakingTransaction
import com.tangem.utils.converter.Converter

class StakingTransactionConverter(
    private val networkTypeConverter: StakingNetworkTypeConverter,
    private val transactionStatusConverter: StakingTransactionStatusConverter,
    private val transactionTypeConverter: StakingTransactionTypeConverter,
    private val gasEstimateConverter: GasEstimateConverter,
) : Converter<StakingTransactionDTO, StakingTransaction> {

    override fun convert(value: StakingTransactionDTO): StakingTransaction {
        return StakingTransaction(
            id = value.id,
            network = networkTypeConverter.convert(value.network),
            status = transactionStatusConverter.convert(value.status),
            type = transactionTypeConverter.convert(value.type),
            hash = value.hash,
            signedTransaction = value.signedTransaction,
            unsignedTransaction = value.unsignedTransaction,
            stepIndex = value.stepIndex,
            error = value.error,
            gasEstimate = value.gasEstimate?.let { gasEstimateConverter.convert(it) },
            stakeId = value.stakeId,
            explorerUrl = value.explorerUrl,
            ledgerHwAppId = value.ledgerHwAppId,
            isMessage = value.isMessage,
        )
    }
}