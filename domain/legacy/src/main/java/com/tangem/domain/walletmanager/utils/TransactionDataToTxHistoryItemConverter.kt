package com.tangem.domain.walletmanager.utils

import com.tangem.blockchain.common.*
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
            interactionAddressType = TxInfo.InteractionAddressType.User(
                address = if (isOutgoing) value.destinationAddress else value.sourceAddress,
            ),
            status = when (value.status) {
                TransactionStatus.Confirmed -> TxInfo.TransactionStatus.Confirmed
                TransactionStatus.Unconfirmed -> TxInfo.TransactionStatus.Unconfirmed
            },
            type = TxInfo.TransactionType.Transfer,
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
}