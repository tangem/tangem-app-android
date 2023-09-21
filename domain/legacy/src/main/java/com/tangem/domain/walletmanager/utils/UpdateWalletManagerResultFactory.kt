package com.tangem.domain.walletmanager.utils

import com.tangem.blockchain.common.*
import com.tangem.blockchain.common.address.Address
import com.tangem.domain.common.extensions.amountToCreateAccount
import com.tangem.domain.txhistory.models.TxHistoryItem
import com.tangem.domain.walletmanager.model.CryptoCurrencyAmount
import com.tangem.domain.walletmanager.model.CryptoCurrencyTransaction
import com.tangem.domain.walletmanager.model.UpdateWalletManagerResult
import timber.log.Timber
import java.math.BigDecimal
import java.util.concurrent.TimeUnit

internal class UpdateWalletManagerResultFactory {

    fun getResult(walletManager: WalletManager): UpdateWalletManagerResult.Verified {
        val wallet = walletManager.wallet
        val addresses = getAvailableAddresses(wallet.addresses)

        return UpdateWalletManagerResult.Verified(
            defaultAddress = wallet.address,
            addresses = addresses,
            currenciesAmounts = getTokensAmounts(wallet.amounts.values.toSet()),
            currentTransactions = getCurrentTransactions(addresses, wallet.recentTransactions.toSet()),
        )
    }

    fun getDemoResult(walletManager: WalletManager, demoAmount: Amount): UpdateWalletManagerResult.Verified {
        val wallet = walletManager.wallet
        val addresses = getAvailableAddresses(wallet.addresses)

        return UpdateWalletManagerResult.Verified(
            defaultAddress = wallet.address,
            addresses = addresses,
            currenciesAmounts = getDemoTokensAmounts(demoAmount, walletManager.cardTokens),
            currentTransactions = getCurrentTransactions(addresses, wallet.recentTransactions.toSet()),
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

    private fun getCurrentTransactions(
        walletAddresses: Set<String>,
        recentTransactions: Set<TransactionData>,
    ): Set<CryptoCurrencyTransaction> {
        val unconfirmedTransactions = recentTransactions.filter {
            it.status == TransactionStatus.Unconfirmed
        }

        return unconfirmedTransactions.mapNotNullTo(hashSetOf()) { createCurrencyTransaction(walletAddresses, it) }
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

    private fun createCurrencyTransaction(
        walletAddresses: Set<String>,
        data: TransactionData,
    ): CryptoCurrencyTransaction? {
        return when (val type = data.amount.type) {
            is AmountType.Coin -> {
                val txHistoryItem = createTxHistoryItem(walletAddresses, data) ?: return null
                CryptoCurrencyTransaction.Coin(txHistoryItem)
            }
            is AmountType.Token -> {
                val txHistoryItem = createTxHistoryItem(walletAddresses, data) ?: return null
                CryptoCurrencyTransaction.Token(
                    tokenId = type.token.id,
                    tokenContractAddress = type.token.contractAddress,
                    txHistoryItem = txHistoryItem,
                )
            }
            is AmountType.Reserve -> null
        }
    }

    private fun createTxHistoryItem(walletAddresses: Set<String>, data: TransactionData): TxHistoryItem? {
        val direction = extractDirection(walletAddresses, data) ?: run {
            Timber.w("Can not determine address for $data")
            return null
        }
        val hash = data.hash ?: return null
        val millis = data.date?.timeInMillis ?: return null
        val amount = getTransactionAmountValue(data.amount) ?: return null

        return TxHistoryItem(
            txHash = hash,
            timestampInMillis = TimeUnit.SECONDS.toMillis(millis),
            direction = direction,
            status = when (data.status) {
                TransactionStatus.Confirmed -> TxHistoryItem.TxStatus.Confirmed
                TransactionStatus.Unconfirmed -> TxHistoryItem.TxStatus.Unconfirmed
            },
            type = TxHistoryItem.TransactionType.Transfer,
            amount = amount,
        )
    }

    private fun extractDirection(
        walletAddresses: Set<String>,
        data: TransactionData,
    ): TxHistoryItem.TransactionDirection? {
        val fromAddress = data.sourceAddress
        val toAddress = data.destinationAddress

        return when {
            toAddress in walletAddresses -> {
                TxHistoryItem.TransactionDirection.Incoming(TxHistoryItem.Address.Single(fromAddress))
            }
            fromAddress in walletAddresses -> {
                TxHistoryItem.TransactionDirection.Outgoing(TxHistoryItem.Address.Single(toAddress))
            }
            else -> {
                Timber.e(
                    """
                    Unable to find transaction direction
                    |- To address: ${data.destinationAddress}
                    |- From address: ${data.sourceAddress}
                    |- Network addresses: $walletAddresses
                    """.trimIndent(),
                )

                return null
            }
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
}