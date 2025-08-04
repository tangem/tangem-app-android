package com.tangem.data.walletmanager.utils

import com.tangem.blockchain.transactionhistory.models.TransactionHistoryItem.TransactionType
import com.tangem.domain.models.network.TxInfo
import com.tangem.domain.walletmanager.model.SmartContractMethod
import com.tangem.utils.converter.Converter

internal class SdkTransactionTypeConverter(
    private val smartContractMethods: Map<String, SmartContractMethod>,
) : Converter<TransactionType, TxInfo.TransactionType> {

    override fun convert(value: TransactionType): TxInfo.TransactionType {
        return when (value) {
            is TransactionType.ContractMethod -> {
                getTransactionType(methodName = smartContractMethods[value.id]?.name)
            }
            is TransactionType.ContractMethodName -> {
                getTransactionType(methodName = value.name)
            }
            is TransactionType.Transfer -> {
                TxInfo.TransactionType.Transfer
            }
            is TransactionType.TronStakingTransactionType.FreezeBalanceV2Contract -> {
                TxInfo.TransactionType.Staking.Stake
            }
            is TransactionType.TronStakingTransactionType.UnfreezeBalanceV2Contract -> {
                TxInfo.TransactionType.Staking.Unstake
            }
            is TransactionType.TronStakingTransactionType.VoteWitnessContract -> {
                TxInfo.TransactionType.Staking.Vote(value.validatorAddress)
            }
            is TransactionType.TronStakingTransactionType.WithdrawBalanceContract -> {
                TxInfo.TransactionType.Staking.ClaimRewards
            }
            is TransactionType.TronStakingTransactionType.WithdrawExpireUnfreezeContract -> {
                TxInfo.TransactionType.Staking.Withdraw
            }
        }
    }

    private fun getTransactionType(methodName: String?): TxInfo.TransactionType {
        return when (methodName) {
            "transfer" -> TxInfo.TransactionType.Transfer
            "approve" -> TxInfo.TransactionType.Approve
            "swap" -> TxInfo.TransactionType.Swap
            "buyVoucher",
            "buyVoucherPOL",
            "delegate",
            -> TxInfo.TransactionType.Staking.Stake
            "sellVoucher_new",
            "sellVoucher_newPOL",
            "undelegate",
            -> TxInfo.TransactionType.Staking.Unstake
            "unstakeClaimTokens_new",
            "unstakeClaimTokens_newPOL",
            "claim",
            -> TxInfo.TransactionType.Staking.Withdraw
            "withdrawRewards",
            "withdrawRewardsPOL",
            -> TxInfo.TransactionType.Staking.ClaimRewards
            "redelegate" -> TxInfo.TransactionType.Staking.Restake
            null -> TxInfo.TransactionType.UnknownOperation
            else -> TxInfo.TransactionType.Operation(name = methodName.replaceFirstChar { it.titlecase() })
        }
    }
}