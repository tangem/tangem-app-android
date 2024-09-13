package com.tangem.domain.walletmanager.utils

import com.tangem.blockchain.transactionhistory.models.TransactionHistoryItem
import com.tangem.domain.txhistory.models.TxHistoryItem
import com.tangem.domain.walletmanager.model.SmartContractMethod
import com.tangem.utils.converter.Converter

internal class SdkTransactionTypeConverter(
    private val smartContractMethods: Map<String, SmartContractMethod>,
) : Converter<TransactionHistoryItem.TransactionType, TxHistoryItem.TransactionType> {

    override fun convert(value: TransactionHistoryItem.TransactionType): TxHistoryItem.TransactionType {
        return when (value) {
            is TransactionHistoryItem.TransactionType.ContractMethod ->
                getTransactionType(methodName = smartContractMethods[value.id]?.name)
            is TransactionHistoryItem.TransactionType.ContractMethodName -> getTransactionType(methodName = value.name)
            is TransactionHistoryItem.TransactionType.Transfer -> TxHistoryItem.TransactionType.Transfer
            TransactionHistoryItem.TransactionType.TronStakingTransactionType.FreezeBalanceV2Contract ->
                TxHistoryItem.TransactionType.Transfer // TODO map correct [REDACTED_JIRA]
            TransactionHistoryItem.TransactionType.TronStakingTransactionType.UnfreezeBalanceV2Contract ->
                TxHistoryItem.TransactionType.Transfer // TODO map correct [REDACTED_JIRA]
            TransactionHistoryItem.TransactionType.TronStakingTransactionType.VoteWitnessContract ->
                TxHistoryItem
                    .TransactionType.Transfer // TODO map correct [REDACTED_JIRA]
            TransactionHistoryItem.TransactionType.TronStakingTransactionType.WithdrawBalanceContract ->
                TxHistoryItem.TransactionType.Transfer // TODO map correct [REDACTED_JIRA]
        }
    }

    private fun getTransactionType(methodName: String?): TxHistoryItem.TransactionType {
        return when (methodName) {
            "transfer" -> TxHistoryItem.TransactionType.Transfer
            "approve" -> TxHistoryItem.TransactionType.Approve
            "swap" -> TxHistoryItem.TransactionType.Swap
            null -> TxHistoryItem.TransactionType.UnknownOperation
            else -> TxHistoryItem.TransactionType.Operation(name = methodName.replaceFirstChar { it.titlecase() })
        }
    }
}