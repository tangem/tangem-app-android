package com.tangem.domain.walletmanager.utils

import com.tangem.blockchain.common.*
import com.tangem.domain.txhistory.models.TxHistoryItem
import com.tangem.domain.walletmanager.model.Address
import com.tangem.utils.converter.Converter
import timber.log.Timber
import java.math.BigDecimal

/**
 * Convert [TransactionData] to [TxHistoryItem]
 *
 * @property walletAddresses wallet addresses
 *
[REDACTED_AUTHOR]
 */
internal class TransactionDataToTxHistoryItemConverter(
    private val walletAddresses: Set<Address>,
    private val feePaidCurrency: FeePaidCurrency,
) : Converter<TransactionData.Uncompiled, TxHistoryItem?> {

    override fun convert(value: TransactionData.Uncompiled): TxHistoryItem? {
        val hash = value.hash ?: return null
        val millis = value.date?.timeInMillis ?: return null
        val amount = getTransactionAmountValue(value.amount, value.fee?.amount) ?: return null
        val isOutgoing = value.sourceAddress in walletAddresses.map(Address::value)

        return TxHistoryItem(
            txHash = hash,
            timestampInMillis = millis,
            isOutgoing = isOutgoing,
            destinationType = TxHistoryItem.DestinationType.Single(
                addressType = TxHistoryItem.AddressType.User(value.destinationAddress),
            ),
            sourceType = TxHistoryItem.SourceType.Single(value.sourceAddress),
            interactionAddressType = TxHistoryItem.InteractionAddressType.User(
                address = if (isOutgoing) value.destinationAddress else value.sourceAddress,
            ),
            status = when (value.status) {
                TransactionStatus.Confirmed -> TxHistoryItem.TransactionStatus.Confirmed
                TransactionStatus.Unconfirmed -> TxHistoryItem.TransactionStatus.Unconfirmed
            },
            type = TxHistoryItem.TransactionType.Transfer,
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