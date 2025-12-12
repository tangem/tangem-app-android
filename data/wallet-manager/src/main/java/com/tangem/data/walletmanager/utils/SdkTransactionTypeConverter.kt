package com.tangem.data.walletmanager.utils

import com.tangem.blockchain.transactionhistory.models.TransactionHistoryItem.TransactionType
import com.tangem.blockchain.yieldsupply.providers.ethereum.yield.EthereumYieldSupplyEnterCallData
import com.tangem.blockchain.yieldsupply.providers.ethereum.yield.EthereumYieldSupplyExitCallData
import com.tangem.blockchain.yieldsupply.providers.ethereum.yield.EthereumYieldSupplyInitTokenCallData
import com.tangem.blockchain.yieldsupply.providers.ethereum.yield.EthereumYieldSupplyReactivateTokenCallData
import com.tangem.domain.models.network.TxInfo
import com.tangem.domain.walletmanager.model.SmartContractMethod
import com.tangem.utils.converter.Converter

internal class SdkTransactionTypeConverter(
    private val smartContractMethods: Map<String, SmartContractMethod>,
) : Converter<Pair<TransactionType, TxInfo.DestinationType>, TxInfo.TransactionType> {

    override fun convert(value: Pair<TransactionType, TxInfo.DestinationType>): TxInfo.TransactionType {
        val (type, destination) = value

        return when (type) {
            is TransactionType.ContractMethod -> {
                getTransactionType(methodName = smartContractMethods[type.id]?.name, type.callData, destination)
            }
            is TransactionType.ContractMethodName -> {
                getTransactionType(methodName = type.name, type.callData, destination)
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
                TxInfo.TransactionType.Staking.Vote(type.validatorAddress)
            }
            is TransactionType.TronStakingTransactionType.WithdrawBalanceContract -> {
                TxInfo.TransactionType.Staking.ClaimRewards
            }
            is TransactionType.TronStakingTransactionType.WithdrawExpireUnfreezeContract -> {
                TxInfo.TransactionType.Staking.Withdraw
            }
        }
    }

    @Suppress("CyclomaticComplexMethod")
    private fun getTransactionType(
        methodName: String?,
        callData: String?,
        destination: TxInfo.DestinationType,
    ): TxInfo.TransactionType {
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
            "yieldSend" -> TxInfo.TransactionType.YieldSupply.Send
            "enterProtocolByOwner" -> callData?.let { data ->
                TxInfo.TransactionType.YieldSupply.Enter(
                    EthereumYieldSupplyEnterCallData.decode(data)?.tokenContractAddress.orEmpty(),
                )
            }
            "withdrawAndDeactivate" -> callData?.let { data ->
                TxInfo.TransactionType.YieldSupply.Exit(
                    EthereumYieldSupplyExitCallData.decode(data)?.tokenContractAddress.orEmpty(),
                )
            }
            "deployYieldModule" -> TxInfo.TransactionType.YieldSupply.DeployContract(
                (destination as? TxInfo.DestinationType.Single)?.addressType?.address.orEmpty(),
            )
            "initYieldToken" -> callData?.let { data ->
                TxInfo.TransactionType.YieldSupply.InitializeToken(
                    EthereumYieldSupplyInitTokenCallData.decode(data)?.tokenContractAddress.orEmpty(),
                )
            }
            "reactivateToken" -> callData?.let { data ->
                TxInfo.TransactionType.YieldSupply.ReactivateToken(
                    EthereumYieldSupplyReactivateTokenCallData.decode(data)?.tokenContractAddress.orEmpty(),
                )
            }
            "supplyTopUp" -> TxInfo.TransactionType.YieldSupply.Topup
            null -> TxInfo.TransactionType.UnknownOperation
            else -> TxInfo.TransactionType.Operation(name = methodName.replaceFirstChar { it.titlecase() })
        } ?: TxInfo.TransactionType.Operation(name = methodName?.replaceFirstChar { it.titlecase() }.orEmpty())
    }
}