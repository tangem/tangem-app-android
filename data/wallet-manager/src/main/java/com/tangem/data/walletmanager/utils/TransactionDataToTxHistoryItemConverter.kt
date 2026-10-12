package com.tangem.data.walletmanager.utils

import com.tangem.blockchain.blockchains.ethereum.EthereumTransactionExtras
import com.tangem.blockchain.blockchains.ethereum.tokenmethods.ApprovalERC20TokenCallData
import com.tangem.blockchain.common.*
import com.tangem.blockchain.yieldsupply.providers.ethereum.factory.EthereumYieldSupplyDeployCallData
import com.tangem.blockchain.yieldsupply.providers.ethereum.yield.EthereumYieldSupplyEnterCallData
import com.tangem.blockchain.yieldsupply.providers.ethereum.yield.EthereumYieldSupplyExitCallData
import com.tangem.blockchain.yieldsupply.providers.ethereum.yield.EthereumYieldSupplyInitTokenCallData
import com.tangem.blockchain.yieldsupply.providers.ethereum.yield.EthereumYieldSupplyReactivateTokenCallData
import com.tangem.blockchainsdk.models.UpdateWalletManagerResult.Address
import com.tangem.domain.models.currency.CryptoCurrency
import com.tangem.domain.models.network.TxInfo
import com.tangem.utils.converter.Converter
import com.tangem.utils.extensions.isZero
import com.tangem.utils.logging.TangemLogger
import java.math.BigDecimal

/**
 * Convert [TransactionData] to [TxInfo]
 *
 * @property walletAddresses wallet addresses
 * @property currency        currency whose history the rows are built for; `null` when they belong to no single
 *                           currency (the wallet-level snapshot), which leaves every type as decoded
 *
[REDACTED_AUTHOR]
 */
internal class TransactionDataToTxHistoryItemConverter(
    private val walletAddresses: Set<Address>,
    private val currency: CryptoCurrency? = null,
) : Converter<TransactionData.Uncompiled, TxInfo?> {

    override fun convert(value: TransactionData.Uncompiled): TxInfo? {
        val hash = value.hash ?: return null
        val millis = value.date?.timeInMillis ?: return null
        val amount = getTransactionAmountValue(value.amount) ?: return null
        val isOutgoing = value.sourceAddress in walletAddresses.map(Address::value)

        return TxInfo.Single(
            txHash = hash,
            timestampInMillis = millis,
            isOutgoing = isOutgoing,
            destinationType = TxInfo.DestinationType.Single(
                addressType = TxInfo.AddressType.User(value.destinationAddress),
            ),
            sourceType = TxInfo.SourceType.Single(value.sourceAddress),
            interactionAddressType = getInteractionAddressType(value, isOutgoing),
            status = when (value.status) {
                TransactionStatus.Confirmed -> TxInfo.TransactionStatus.Confirmed
                TransactionStatus.Unconfirmed -> TxInfo.TransactionStatus.Unconfirmed
            },
            type = getTransactionType(value).orCoinFeeOnly(isOutgoing = isOutgoing, moved = value.amount.value),
            amount = amount,
            fee = value.fee?.amount?.toDomain(),
        )
    }

    /**
     * What the transaction moves, with the fee left out — [TxInfo.fee] carries that separately, and the confirmed
     * row the explorer later reports counts the same way, so the row keeps its value once it is confirmed.
     */
    private fun getTransactionAmountValue(amount: Amount): BigDecimal? {
        val value = amount.value

        if (value == null) {
            TangemLogger.w("Transaction amount must not be null: ${amount.currencySymbol}")
        }

        return value
    }

    private fun getInteractionAddressType(
        value: TransactionData.Uncompiled,
        isOutgoing: Boolean,
    ): TxInfo.InteractionAddressType = when (val extras = value.extras) {
        is EthereumTransactionExtras -> {
            when (val callData = extras.callData) {
                is ApprovalERC20TokenCallData -> TxInfo.InteractionAddressType.Contract(
                    address = callData.spenderAddress,
                )
                else -> TxInfo.InteractionAddressType.User(
                    address = if (isOutgoing) value.destinationAddress else value.sourceAddress,
                )
            }
        }
        else -> TxInfo.InteractionAddressType.User(
            address = if (isOutgoing) value.destinationAddress else value.sourceAddress,
        )
    }

    /**
     * A protocol call describes what the token did. In the coin's own history the wallet moved no coin and paid
     * nothing but the fee, so the row reads as a plain operation — the same fallback the confirmed row takes in
     * [SdkTransactionTypeConverter], so the row does not change shape once the explorer reports it. The call itself
     * rides along as [TxInfo.TransactionType.Operation.recognized]: the fee row names what it paid for, and names it
     * the same before and after the explorer reports the transaction. The method name stays empty — here the call is
     * decoded from its call data, which carries the selector and not the name the explorer would give it.
     */
    private fun TxInfo.TransactionType.orCoinFeeOnly(isOutgoing: Boolean, moved: BigDecimal?): TxInfo.TransactionType {
        val isCoinFeeOnly = currency is CryptoCurrency.Coin &&
            isOutgoing &&
            moved?.isZero() == true &&
            this is TxInfo.TransactionType.YieldSupply

        return if (isCoinFeeOnly) TxInfo.TransactionType.Operation(name = "", recognized = this) else this
    }

    private fun getTransactionType(transactionData: TransactionData.Uncompiled?): TxInfo.TransactionType {
        return when (val extras = transactionData?.extras) {
            is EthereumTransactionExtras -> {
                when (val callData = extras.callData) {
                    is EthereumYieldSupplyDeployCallData -> TxInfo.TransactionType.YieldSupply.DeployContract(
                        transactionData.destinationAddress,
                    )
                    is EthereumYieldSupplyReactivateTokenCallData -> TxInfo.TransactionType.YieldSupply.ReactivateToken(
                        callData.tokenContractAddress,
                    )
                    is EthereumYieldSupplyInitTokenCallData -> TxInfo.TransactionType.YieldSupply.InitializeToken(
                        callData.tokenContractAddress,
                    )
                    is EthereumYieldSupplyEnterCallData -> TxInfo.TransactionType.YieldSupply.Enter(
                        callData.tokenContractAddress,
                    )
                    is EthereumYieldSupplyExitCallData -> TxInfo.TransactionType.YieldSupply.Exit(
                        callData.tokenContractAddress,
                    )
                    // An unlimited approval carries no allowance in the call data; the tx amount still names the token.
                    is ApprovalERC20TokenCallData -> TxInfo.TransactionType.Approve(
                        amount = callData.amount?.toDomain() ?: transactionData.amount.toDomain().copy(value = null),
                        address = callData.spenderAddress,
                    )
                    else -> TxInfo.TransactionType.Transfer
                }
            }
            else -> TxInfo.TransactionType.Transfer
        }
    }
}