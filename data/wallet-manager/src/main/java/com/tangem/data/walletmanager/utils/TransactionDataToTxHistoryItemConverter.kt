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
import com.tangem.domain.models.network.TxInfo
import com.tangem.utils.converter.Converter
import timber.log.Timber
import java.math.BigDecimal

/**
 * Convert [TransactionData] to [TxInfo]
 *
 * @property walletAddresses wallet addresses
 *
[REDACTED_AUTHOR]
 */
internal class TransactionDataToTxHistoryItemConverter(
    private val walletAddresses: Set<Address>,
    private val feePaidCurrency: FeePaidCurrency,
) : Converter<TransactionData.Uncompiled, TxInfo?> {

    override fun convert(value: TransactionData.Uncompiled): TxInfo? {
        val hash = value.hash ?: return null
        val millis = value.date?.timeInMillis ?: return null
        val amount = getTransactionAmountValue(value.amount, value.fee?.amount) ?: return null
        val isOutgoing = value.sourceAddress in walletAddresses.map(Address::value)

        return TxInfo(
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
            type = getTransactionType(value),
            amount = amount,
        )
    }

    private fun getTransactionAmountValue(amount: Amount, feeAmount: Amount?): BigDecimal? {
        val feeValue = feeAmount?.value ?: BigDecimal.ZERO
        val value = amount.value

        if (value == null) {
            Timber.w("Transaction amount must not be null: ${amount.currencySymbol}")
        }

        return when (feePaidCurrency) {
            FeePaidCurrency.SameCurrency -> value?.plus(feeValue)
            FeePaidCurrency.Coin -> {
                if (amount.type is AmountType.Coin) value?.plus(feeValue) else value
            }
            is FeePaidCurrency.Token -> {
                val token = (amount.type as? AmountType.Token)?.token ?: return value
                if (isSameToken(token, feePaidCurrency.token)) {
                    value?.plus(feeValue)
                } else {
                    value
                }
            }
            is FeePaidCurrency.FeeResource -> value
        }
    }

    private fun isSameToken(amountToken: Token, feeToken: Token): Boolean {
        return amountToken.contractAddress.equals(feeToken.contractAddress, ignoreCase = true) &&
            amountToken.symbol.equals(feeToken.symbol, ignoreCase = true)
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
                    is ApprovalERC20TokenCallData -> TxInfo.TransactionType.Approve
                    else -> TxInfo.TransactionType.Transfer
                }
            }
            else -> TxInfo.TransactionType.Transfer
        }
    }
}