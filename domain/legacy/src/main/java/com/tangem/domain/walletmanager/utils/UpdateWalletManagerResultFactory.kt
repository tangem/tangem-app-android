package com.tangem.domain.walletmanager.utils

import com.tangem.blockchain.common.*
import com.tangem.blockchain.common.address.Address
import com.tangem.domain.common.extensions.amountToCreateAccount
import com.tangem.domain.walletmanager.model.CryptoCurrencyAmount
import com.tangem.domain.walletmanager.model.CryptoCurrencyTransaction
import com.tangem.domain.walletmanager.model.UpdateWalletManagerResult
import org.joda.time.DateTime
import org.joda.time.DateTimeZone
import org.joda.time.Instant
import timber.log.Timber
import java.math.BigDecimal
import java.util.Calendar

internal class UpdateWalletManagerResultFactory {

    fun getResult(walletManager: WalletManager): UpdateWalletManagerResult.Verified {
        val wallet = walletManager.wallet

        return UpdateWalletManagerResult.Verified(
            defaultAddress = wallet.address,
            addresses = getAvailableAddresses(wallet.addresses),
            currenciesAmounts = getTokensAmounts(wallet.amounts.values.toSet()),
            currentTransactions = getCurrentTransactions(wallet.recentTransactions.toSet()),
        )
    }

    fun getDemoResult(walletManager: WalletManager, demoAmount: Amount): UpdateWalletManagerResult.Verified {
        val wallet = walletManager.wallet

        return UpdateWalletManagerResult.Verified(
            defaultAddress = wallet.address,
            addresses = getAvailableAddresses(wallet.addresses),
            currenciesAmounts = getDemoTokensAmounts(demoAmount, walletManager.cardTokens),
            currentTransactions = getCurrentTransactions(wallet.recentTransactions.toSet()),
        )
    }

    fun getNoAccountResult(walletManager: WalletManager): UpdateWalletManagerResult.NoAccount {
        val wallet = walletManager.wallet
        val blockchain = wallet.blockchain
        val amountToCreateAccount = blockchain.amountToCreateAccount(
            token = wallet.getTokens().firstOrNull(),
        )

        requireNotNull(amountToCreateAccount) {
            "Unable to get required amount to create account for: $blockchain"
        }

        return UpdateWalletManagerResult.NoAccount(
            defaultAddress = wallet.address,
            addresses = getAvailableAddresses(wallet.addresses),
            amountToCreateAccount = amountToCreateAccount,
        )
    }

    private fun getTokensAmounts(amounts: Set<Amount>): Set<CryptoCurrencyAmount> {
        val mutableAmounts = hashSetOf<CryptoCurrencyAmount>()

        return amounts.mapNotNullTo(mutableAmounts, ::createCurrencyAmount)
    }

    private fun getDemoTokensAmounts(demoAmount: Amount, tokens: Set<Token>): Set<CryptoCurrencyAmount> {
        val amountValue = demoAmount.value ?: BigDecimal.ZERO
        val demoAmounts = hashSetOf<CryptoCurrencyAmount>(CryptoCurrencyAmount.Coin(amountValue))

        return tokens.mapTo(demoAmounts) { token ->
            CryptoCurrencyAmount.Token(token.id, token.contractAddress, amountValue)
        }
    }

    private fun getCurrentTransactions(recentTransactions: Set<TransactionData>): Set<CryptoCurrencyTransaction> {
        val unconfirmedTransactions = recentTransactions.filter {
            it.status == TransactionStatus.Unconfirmed
        }

        return unconfirmedTransactions.mapNotNullTo(hashSetOf(), ::createCurrencyTransaction)
    }

    private fun createCurrencyAmount(amount: Amount): CryptoCurrencyAmount? {
        return when (val type = amount.type) {
            is AmountType.Token -> CryptoCurrencyAmount.Token(
                tokenId = type.token.id,
                tokenContractAddress = type.token.contractAddress,
                value = getCurrencyAmountValue(amount) ?: return null,
            )
            is AmountType.Coin -> CryptoCurrencyAmount.Coin(
                value = getCurrencyAmountValue(amount) ?: return null,
            )
            is AmountType.Reserve -> null
        }
    }

    private fun createCurrencyTransaction(data: TransactionData): CryptoCurrencyTransaction? {
        val fromAddress = takeAddressIfNotUnknown(data.sourceAddress)
        val toAddress = takeAddressIfNotUnknown(data.destinationAddress)
        val amount = getTransactionAmountValue(data.amount) ?: return null
        val sentAt = getTransactionSentTime(data.date) ?: return null

        return when (val type = data.amount.type) {
            is AmountType.Coin -> CryptoCurrencyTransaction.Coin(
                amount = amount,
                fromAddress = fromAddress,
                toAddress = toAddress,
                sentAt = sentAt,
            )
            is AmountType.Token -> CryptoCurrencyTransaction.Token(
                tokenId = type.token.id,
                tokenContractAddress = type.token.contractAddress,
                amount = amount,
                fromAddress = fromAddress,
                toAddress = toAddress,
                sentAt = sentAt,
            )
            is AmountType.Reserve -> null
        }
    }

    private fun getAvailableAddresses(addresses: Set<Address>): Set<String> {
        return addresses.mapTo(hashSetOf()) { it.value }
    }

    private fun getCurrencyAmountValue(amount: Amount): BigDecimal? {
        val value = amount.value

        if (value == null) {
            Timber.e("Currency amount must not be null: ${amount.currencySymbol}")
        }

        return value
    }

    private fun getTransactionAmountValue(amount: Amount): BigDecimal? {
        val value = amount.value

        if (value == null) {
            Timber.e("Transaction amount must not be null: ${amount.currencySymbol}")
        }

        return value
    }

    private fun getTransactionSentTime(date: Calendar?): DateTime? {
        if (date == null) {
            Timber.e("Transaction date must not be null")
            return null
        }

        val instant = Instant.ofEpochMilli(date.timeInMillis)
        val timeZone = DateTimeZone.forTimeZone(date.timeZone)

        return instant.toDateTime(timeZone)
    }

    private fun takeAddressIfNotUnknown(address: String): String? {
        return address.takeIf { it.isNotBlank() && it != UNKNOWN_TRANSACTION_ADDRESS }
    }

    private companion object {
        const val UNKNOWN_TRANSACTION_ADDRESS = "unknown"
    }
}