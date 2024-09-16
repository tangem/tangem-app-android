package com.tangem.domain.walletmanager.utils

import com.tangem.blockchain.transactionhistory.models.TransactionHistoryItem.TransactionType
import com.tangem.domain.txhistory.models.TxHistoryItem
import com.tangem.domain.walletmanager.model.SmartContractMethod
import com.tangem.utils.converter.Converter

internal class SdkTransactionTypeConverter(
    private val smartContractMethods: Map<String, SmartContractMethod>,
) : Converter<TransactionType, TxHistoryItem.TransactionType> {

    override fun convert(value: TransactionType): TxHistoryItem.TransactionType {
        return when (value) {
            is TransactionType.ContractMethod -> {
                getTransactionType(methodName = smartContractMethods[value.id]?.name)
            }
            is TransactionType.ContractMethodName -> {
                getTransactionType(methodName = value.name)
            }
            is TransactionType.Transfer -> {
                TxHistoryItem.TransactionType.Transfer
            }
            is TransactionType.TronStakingTransactionType.FreezeBalanceV2Contract -> {
                TxHistoryItem.TransactionType.TronStakingTransactionType.Stake
            }
            is TransactionType.TronStakingTransactionType.UnfreezeBalanceV2Contract -> {
                TxHistoryItem.TransactionType.TronStakingTransactionType.Unstake
            }
            is TransactionType.TronStakingTransactionType.VoteWitnessContract -> {
                TxHistoryItem.TransactionType.TronStakingTransactionType.Vote
            }
            is TransactionType.TronStakingTransactionType.WithdrawBalanceContract -> {
                TxHistoryItem.TransactionType.TronStakingTransactionType.Withdraw
            }
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