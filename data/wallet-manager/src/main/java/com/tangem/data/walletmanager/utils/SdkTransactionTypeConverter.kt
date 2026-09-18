package com.tangem.data.walletmanager.utils

import com.tangem.blockchain.blockchains.ethereum.tokenmethods.ApprovalERC20TokenCallData
import com.tangem.blockchain.blockchains.ethereum.tokenmethods.TransferERC20TokenCallData
import com.tangem.blockchain.extensions.formatHex
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
    smartContractMethods: Map<String, SmartContractMethod>,
    private val yieldSupplyAddresses: Set<String>,
    gaslessFeeAddresses: Set<String>,
    private val currency: CryptoCurrency,
    private val networkTokens: Set<CryptoCurrency.Token> = emptySet(),
) : Converter<TransactionHistoryItem, TxInfo.TransactionType> {

    /** The SDK reports a selector prefixed or bare depending on the source; the asset is keyed prefixed. */
    private val smartContractMethods: Map<String, SmartContractMethod> =
        smartContractMethods.mapKeys { (selector, _) -> selector.toSelectorKey() }

    private val gaslessFeeAddressesLowercase: Set<String> = gaslessFeeAddresses.map { it.lowercase() }.toSet()

    override fun convert(value: TransactionHistoryItem): TxInfo.TransactionType {
        val (type, destination) = value.type to value.destinationType
        val source = value.sourceType

        return when (type) {
            is TransactionType.ContractMethod -> {
                getTransactionType(
                    methodName = smartContractMethods[type.id.toSelectorKey()]?.name,
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
            "transfer" -> decodeTokenTransfer(callData, destination) ?: TxInfo.TransactionType.Transfer
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
     * A token transfer reaches the coin history only because the wallet paid the gas for it, so it is reported as the
     * fee it cost. In the token's own history the same transaction *is* the transfer and stays one (`null` here).
     */
    private fun decodeTokenTransfer(
        callData: String?,
        destination: TransactionHistoryItem.DestinationType,
    ): TxInfo.TransactionType.NetworkFee? {
        if (currency !is CryptoCurrency.Coin) return null
        val data = callData ?: return null
        val transfer = TransferERC20TokenCallData(data) ?: return null
        return TxInfo.TransactionType.NetworkFee(
            amount = destination.resolveToken()?.toSdkAmount(rawValue = transfer.amount.value),
            address = transfer.destination,
        )
    }

    /**
     * The SDK decodes the allowance raw, in the token's smallest units (`null` for an unlimited one), and knows
     * nothing about the token itself. The viewed [currency] is the fallback for a token the wallet does not track.
     */
    private fun toAllowance(rawAllowance: BigDecimal?, destination: TransactionHistoryItem.DestinationType): SdkAmount {
        val token = destination.resolveToken() ?: return SdkAmount(
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
        return token.toSdkAmount(rawValue = rawAllowance)
    }

    /** A token call is sent to the token's own contract, so the destination identifies it among [networkTokens]. */
    private fun TransactionHistoryItem.DestinationType.resolveToken(): CryptoCurrency.Token? {
        val contractAddress = (this as? TransactionHistoryItem.DestinationType.Single)?.addressType?.address
            ?: return null
        return networkTokens.find { it.contractAddress.equals(contractAddress, ignoreCase = true) }
    }

    private fun CryptoCurrency.Token.toSdkAmount(rawValue: BigDecimal?): SdkAmount = SdkAmount(
        currencySymbol = symbol,
        value = rawValue?.movePointLeft(decimals),
        decimals = decimals,
        type = SdkAmountType.Token(contractAddress = contractAddress, id = id.rawCurrencyId?.value),
    )

    private fun String.toSelectorKey(): String = lowercase().formatHex()

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