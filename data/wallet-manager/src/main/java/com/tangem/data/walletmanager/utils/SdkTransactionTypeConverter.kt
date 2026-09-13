package com.tangem.data.walletmanager.utils

import com.tangem.blockchain.blockchains.ethereum.tokenmethods.ApprovalERC20TokenCallData
import com.tangem.blockchain.common.Token
import com.tangem.blockchain.transactionhistory.models.TransactionHistoryItem
import com.tangem.blockchain.transactionhistory.models.TransactionHistoryItem.TransactionType
import com.tangem.blockchain.yieldsupply.providers.ethereum.yield.EthereumYieldSupplyEnterCallData
import com.tangem.blockchain.yieldsupply.providers.ethereum.yield.EthereumYieldSupplyExitCallData
import com.tangem.blockchain.yieldsupply.providers.ethereum.yield.EthereumYieldSupplyInitTokenCallData
import com.tangem.blockchain.yieldsupply.providers.ethereum.yield.EthereumYieldSupplyReactivateTokenCallData
import com.tangem.common.extensions.hexToBytes
import com.tangem.domain.models.currency.CryptoCurrency
import com.tangem.domain.models.network.SdkAmount
import com.tangem.domain.models.network.SdkAmountType
import com.tangem.domain.models.network.TxInfo
import com.tangem.domain.walletmanager.model.SmartContractMethod
import com.tangem.utils.converter.Converter
import java.math.BigDecimal

internal class SdkTransactionTypeConverter(
    private val smartContractMethods: Map<String, SmartContractMethod>,
    private val yieldSupplyAddresses: Set<String>,
    gaslessFeeAddresses: Set<String>,
    private val currency: CryptoCurrency,
    private val networkTokens: Set<Token> = emptySet(),
) : Converter<TransactionHistoryItem, TxInfo.TransactionType> {

    private val gaslessFeeAddressesLowercase: Set<String> = gaslessFeeAddresses.map { it.lowercase() }.toSet()

    override fun convert(value: TransactionHistoryItem): TxInfo.TransactionType {
        val (type, destination) = value.type to value.destinationType
        val source = value.sourceType

        return when (type) {
            is TransactionType.ContractMethod -> {
                getTransactionType(
                    methodName = smartContractMethods[type.id]?.name,
                    callData = type.callData,
                    destination = destination,
                    source = source,
                )
            }
            is TransactionType.ContractMethodName -> {
                getTransactionType(
                    methodName = type.name,
                    callData = type.callData,
                    destination = destination,
                    source = source,
                )
            }
            is TransactionType.Transfer -> {
                TxInfo.TransactionType.Transfer
            }
            is TransactionType.SolanaStakingTransactionType.Stake -> {
                TxInfo.TransactionType.Staking.Stake
            }
            is TransactionType.SolanaStakingTransactionType.Unstake -> {
                TxInfo.TransactionType.Staking.Unstake
            }
            is TransactionType.SolanaStakingTransactionType.Withdraw -> {
                TxInfo.TransactionType.Staking.Withdraw
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
        destination: TransactionHistoryItem.DestinationType,
        source: TransactionHistoryItem.SourceType,
    ): TxInfo.TransactionType {
        return when (methodName) {
            "transfer" -> TxInfo.TransactionType.Transfer
            "approve" -> decodeApprove(callData, destination)
            "swap" -> TxInfo.TransactionType.Swap
            "buyVoucher",
            "buyVoucherPOL",
            "delegate",
            "pooledStake",
            -> TxInfo.TransactionType.Staking.Stake
            "sellVoucher_new",
            "sellVoucher_newPOL",
            "undelegate",
            "pooledUnstake",
            -> TxInfo.TransactionType.Staking.Unstake
            "unstakeClaimTokens_new",
            "unstakeClaimTokens_newPOL",
            "claim",
            -> TxInfo.TransactionType.Staking.Withdraw
            "withdrawRewards",
            "withdrawRewardsPOL",
            -> TxInfo.TransactionType.Staking.ClaimRewards
            "redelegate" -> TxInfo.TransactionType.Staking.Restake
            "yieldSend" -> {
                val sourceAddresses = when (source) {
                    is TransactionHistoryItem.SourceType.Multiple -> source.addresses
                    is TransactionHistoryItem.SourceType.Single -> listOf(source.address)
                }.map {
                    it.lowercase()
                }.toSet()

                val isYieldSupplyWithdraw =
                    yieldSupplyAddresses.intersect(sourceAddresses).isNotEmpty()

                TxInfo.TransactionType.YieldSupply.Send(
                    isYieldSupplyWithdraw = isYieldSupplyWithdraw,
                )
            }
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
                (destination as? TransactionHistoryItem.DestinationType.Single)?.addressType?.address.orEmpty(),
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
            "gaslessTransaction" -> getTypeForGaslessMethod(destination)
            null -> TxInfo.TransactionType.UnknownOperation
            else -> TxInfo.TransactionType.Operation(name = methodName.replaceFirstChar { it.titlecase() })
        } ?: TxInfo.TransactionType.Operation(name = methodName?.replaceFirstChar { it.titlecase() }.orEmpty())
    }

    private fun decodeApprove(
        callData: String?,
        destination: TransactionHistoryItem.DestinationType,
    ): TxInfo.TransactionType.Approve? {
        val data = callData ?: return null
        val approval = ApprovalERC20TokenCallData(data.hexToBytes()) ?: return null
        return TxInfo.TransactionType.Approve(
            amount = toAllowance(rawAllowance = approval.amount?.value, destination = destination),
            address = approval.spenderAddress,
        )
    }

    /**
     * The SDK decodes the allowance as a raw `uint256` in the token's smallest units (`null` for an unlimited one) and
     * knows nothing about the token itself (`Blockchain.Unknown`: empty symbol, zero decimals). ERC-20 `approve` is
     * always sent to the token contract, so the tx destination identifies the approved token among the wallet's
     * [networkTokens]; the viewed [currency] is the fallback for a contract the wallet does not track.
     */
    private fun toAllowance(rawAllowance: BigDecimal?, destination: TransactionHistoryItem.DestinationType): SdkAmount {
        val contractAddress = (destination as? TransactionHistoryItem.DestinationType.Single)?.addressType?.address
        val token = contractAddress?.let { address ->
            networkTokens.find { it.contractAddress.equals(address, ignoreCase = true) }
        }
        return if (token != null) {
            SdkAmount(
                currencySymbol = token.symbol,
                value = rawAllowance?.movePointLeft(token.decimals),
                decimals = token.decimals,
                type = SdkAmountType.Token(contractAddress = token.contractAddress, id = token.id),
            )
        } else {
            SdkAmount(
                currencySymbol = currency.symbol,
                value = rawAllowance?.movePointLeft(currency.decimals),
                decimals = currency.decimals,
                type = when (currency) {
                    is CryptoCurrency.Coin -> SdkAmountType.Coin
                    is CryptoCurrency.Token -> SdkAmountType.Token(
                        contractAddress = currency.contractAddress,
                        id = currency.id.rawCurrencyId?.value,
                    )
                },
            )
        }
    }

    private fun getTypeForGaslessMethod(destination: TransactionHistoryItem.DestinationType): TxInfo.TransactionType {
        return when (destination) {
            is TransactionHistoryItem.DestinationType.Multiple -> TxInfo.TransactionType.UnknownOperation
            is TransactionHistoryItem.DestinationType.Single -> {
                val address = destination.addressType.address.lowercase()
                if (gaslessFeeAddressesLowercase.contains(address)) {
                    TxInfo.TransactionType.GaslessFee
                } else {
                    TxInfo.TransactionType.UnknownOperation
                }
            }
        }
    }
}