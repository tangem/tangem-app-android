package com.tangem.data.staking.converters.transaction

import com.tangem.datasource.api.stakekit.models.response.model.transaction.StakingTransactionDTO
import com.tangem.datasource.local.token.converter.StakingNetworkTypeConverter
import com.tangem.domain.staking.model.stakekit.transaction.StakingTransaction
import com.tangem.utils.converter.Converter

class StakingTransactionConverter(
    private val transactionStatusConverter: StakingTransactionStatusConverter,
    private val transactionTypeConverter: StakingTransactionTypeConverter,
) : Converter<StakingTransactionDTO, StakingTransaction> {

    override fun convert(value: StakingTransactionDTO): StakingTransaction {
        return StakingTransaction(
            id = value.id,
            network = StakingNetworkTypeConverter.convert(value.network),
            status = transactionStatusConverter.convert(value.status),
            type = transactionTypeConverter.convert(value.type),
            hash = value.hash,
            signedTransaction = value.signedTransaction,
            unsignedTransaction = value.unsignedTransaction,
            stepIndex = value.stepIndex,
            error = value.error,
            gasEstimate = value.gasEstimate?.let(GasEstimateConverter::convert),
            stakeId = value.stakeId,
            explorerUrl = value.explorerUrl,
            ledgerHwAppId = value.ledgerHwAppId,
            isMessage = value.isMessage,
        )
    }
}