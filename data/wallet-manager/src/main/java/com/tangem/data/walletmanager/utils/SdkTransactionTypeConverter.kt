package com.tangem.data.walletmanager.utils

import com.tangem.blockchain.blockchains.ethereum.tokenmethods.ApprovalERC20TokenCallData
import com.tangem.blockchain.blockchains.ethereum.tokenmethods.TransferERC20TokenCallData
import com.tangem.blockchain.common.HEX_PREFIX
import com.tangem.blockchain.extensions.formatHex
import com.tangem.blockchain.transactionhistory.models.TransactionHistoryItem
import com.tangem.blockchain.transactionhistory.models.TransactionHistoryItem.TransactionType
import com.tangem.blockchain.yieldsupply.providers.ethereum.yield.EthereumYieldSupplyEnterCallData
import com.tangem.blockchain.yieldsupply.providers.ethereum.yield.EthereumYieldSupplyExitCallData
import com.tangem.blockchain.yieldsupply.providers.ethereum.yield.EthereumYieldSupplyInitTokenCallData
import com.tangem.blockchain.yieldsupply.providers.ethereum.yield.EthereumYieldSupplyReactivateTokenCallData
import com.tangem.common.extensions.hexToBytes
import com.tangem.domain.express.models.ExpressAsset
import com.tangem.domain.jointaccount.gas.SafeTxGasBuilder
import com.tangem.domain.jointaccount.model.SafeExecution
import com.tangem.domain.jointaccount.model.SafeTx
import com.tangem.domain.jointaccount.safe.SafeExecTransactionDecoder
import com.tangem.domain.models.currency.CryptoCurrency
import com.tangem.domain.models.network.Network
import com.tangem.domain.models.network.SdkAmount
import com.tangem.domain.models.network.SdkAmountType
import com.tangem.domain.models.network.TxInfo
import com.tangem.domain.txhistory.usecase.GetCachedTokenInfoUseCase
import com.tangem.domain.walletmanager.model.SmartContractMethod
import com.tangem.utils.converter.Converter
import com.tangem.utils.extensions.isZero
import java.math.BigDecimal

internal class SdkTransactionTypeConverter(
    smartContractMethods: Map<String, SmartContractMethod>,
    private val yieldSupplyAddresses: Set<String>,
    gaslessFeeAddresses: Set<String>,
    private val currency: CryptoCurrency,
    private val networkTokens: Set<CryptoCurrency.Token> = emptySet(),
    private val getCachedTokenInfoUseCase: GetCachedTokenInfoUseCase,
    private val safeAddress: String? = null,
) : Converter<TransactionHistoryItem, TxInfo.TransactionType> {

    private val isJointAccount = currency.network.derivationPath is Network.DerivationPath.Joint

    /** The SDK reports a selector prefixed or bare depending on the source; the asset is keyed prefixed. */
    private val smartContractMethods: Map<String, SmartContractMethod> =
        smartContractMethods.mapKeys { (selector, _) -> selector.toSelectorKey() }

    private val gaslessFeeAddressesLowercase: Set<String> = gaslessFeeAddresses.map { it.lowercase() }.toSet()

    override fun convert(value: TransactionHistoryItem): TxInfo.TransactionType {
        return when (val type = value.type) {
            is TransactionType.ContractMethod -> {
                getTransactionType(
                    methodName = smartContractMethods[type.id.toSelectorKey()]?.name,
                    callData = type.callData,
                    historyItem = value,
                )
            }
            is TransactionType.ContractMethodName -> {
                getTransactionType(
                    methodName = type.name,
                    callData = type.callData,
                    historyItem = value,
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
        historyItem: TransactionHistoryItem,
    ): TxInfo.TransactionType {
        val destination = historyItem.destinationType
        val source = historyItem.sourceType
        var result = when (methodName) {
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
            "yieldSend" -> decodeYieldSend(source)
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
            "gaslessTransaction" -> getTypeForGaslessMethod(destination, source, methodName)
            "execTransaction",
            "aggregate3",
            -> decodeSafeExecution(callData, destination)
            else -> fallbackOperation(methodName)
        } ?: fallbackOperation(methodName)

        // A call decoded into a protocol interaction (yield supply for now, other protocols may join) describes what
        // the token did. In the coin's own history the same call moved nothing and cost only the fee, so it drops
        // back to a plain operation — which the history then renders as a network-fee row.
        val isCoinFeeOnly = currency is CryptoCurrency.Coin &&
            historyItem.amount.value?.isZero() == true &&
            result is TxInfo.TransactionType.YieldSupply
        if (isCoinFeeOnly) {
            result = fallbackOperation(methodName)
        }
        return result
    }

    private fun fallbackOperation(methodName: String?): TxInfo.TransactionType {
        return if (methodName == null) {
            TxInfo.TransactionType.UnknownOperation
        } else {
            TxInfo.TransactionType.Operation(name = methodName.replaceFirstChar { it.titlecase() })
        }
    }

    private fun decodeYieldSend(source: TransactionHistoryItem.SourceType): TxInfo.TransactionType.YieldSupply.Send {
        val sourceAddresses = when (source) {
            is TransactionHistoryItem.SourceType.Multiple -> source.addresses.map { it.lowercase() }.toSet()
            is TransactionHistoryItem.SourceType.Single -> setOf(source.address.lowercase())
        }

        val isYieldSupplyWithdraw =
            yieldSupplyAddresses.intersect(sourceAddresses).isNotEmpty()

        return TxInfo.TransactionType.YieldSupply.Send(
            isYieldSupplyWithdraw = isYieldSupplyWithdraw,
        )
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
        val result = networkTokens.find { it.contractAddress.equals(contractAddress, ignoreCase = true) }
        if (result == null) {
            val tokenId = ExpressAsset.ID(
                networkId = currency.network.rawId,
                contractAddress = contractAddress,
            )
            getCachedTokenInfoUseCase.fetchMissing(setOf(tokenId))
        }
        return result
    }

    private fun CryptoCurrency.Token.toSdkAmount(rawValue: BigDecimal?): SdkAmount = SdkAmount(
        currencySymbol = symbol,
        value = rawValue?.movePointLeft(decimals),
        decimals = decimals,
        type = SdkAmountType.Token(contractAddress = contractAddress, id = id.rawCurrencyId?.value),
    )

    private fun String.toSelectorKey(): String = lowercase().formatHex()

    /**
     * The Safe execution in [callData] when this joint account's own Safe ran it: another Safe paying this account
     * lands in its history too, and a refund in an ERC-20 gas token cannot be shown as the network fee.
     */
    private fun readOwnSafeExecution(
        callData: String?,
        destination: TransactionHistoryItem.DestinationType,
    ): SafeExecution? {
        if (!isJointAccount) return null
        val execution = SafeExecTransactionDecoder.decode(callData ?: return null) ?: return null
        val executingSafe = execution.safe
            ?: (destination as? TransactionHistoryItem.DestinationType.Single)?.addressType?.address
        return execution.takeIf {
            execution.operation == SafeTx.OPERATION_CALL &&
                execution.refund.gasToken.equals(SafeTxGasBuilder.NATIVE_GAS_TOKEN, ignoreCase = true) &&
                safeAddress != null && safeAddress.equals(executingSafe, ignoreCase = true)
        }
    }

    private fun decodeSafeExecution(
        callData: String?,
        destination: TransactionHistoryItem.DestinationType,
    ): TxInfo.TransactionType.Joint? {
        val execution = readOwnSafeExecution(callData, destination) ?: return null

        val call = execution.call
        val tokenTransfer = TransferERC20TokenCallData(compiledData = call.data)
        val refundCapWei = execution.refund.maxPayment.toBigDecimal()
        val isNativeTransfer = call.data.removePrefix(HEX_PREFIX).isEmpty() && call.value.signum() > 0
        return when {
            currency is CryptoCurrency.Token -> tokenTransfer?.let { transfer ->
                TxInfo.TransactionType.Joint.Transfer(
                    recipient = transfer.destination,
                    amount = transfer.amount.value?.movePointLeft(currency.decimals) ?: return null,
                    refundCapWei = refundCapWei,
                    signaturesCount = execution.signaturesCount,
                )
            }
            isNativeTransfer -> TxInfo.TransactionType.Joint.Transfer(
                recipient = call.to,
                amount = call.value.toBigDecimal().movePointLeft(currency.decimals),
                refundCapWei = refundCapWei,
                signaturesCount = execution.signaturesCount,
            )
            tokenTransfer != null && call.value.signum() == 0 -> TxInfo.TransactionType.Joint.TokenFee(
                tokenContract = call.to,
                tokenAmount = networkTokens
                    .firstOrNull { it.contractAddress.equals(call.to, ignoreCase = true) }
                    ?.toSdkAmount(rawValue = tokenTransfer.amount.value),
                recipient = tokenTransfer.destination,
                refundCapWei = refundCapWei,
                signaturesCount = execution.signaturesCount,
            )
            else -> TxInfo.TransactionType.Joint.Call(
                contract = call.to,
                value = call.value.toBigDecimal().movePointLeft(currency.decimals),
                refundCapWei = refundCapWei,
                signaturesCount = execution.signaturesCount,
            )
        }
    }

    private fun getTypeForGaslessMethod(
        destination: TransactionHistoryItem.DestinationType,
        source: TransactionHistoryItem.SourceType,
        methodName: String,
    ): TxInfo.TransactionType {
        return when {
            destination is TransactionHistoryItem.DestinationType.Single &&
                gaslessFeeAddressesLowercase.contains(destination.addressType.address.lowercase())
            -> TxInfo.TransactionType.GaslessFee
            source is TransactionHistoryItem.SourceType.Single &&
                yieldSupplyAddresses.contains(source.address.lowercase())
            -> TxInfo.TransactionType.YieldSupply.Send(isYieldSupplyWithdraw = true)

            else -> fallbackOperation(methodName)
        }
    }
}