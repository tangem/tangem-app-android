package com.tangem.domain.walletmanager.utils

import com.tangem.blockchain.common.*
import com.tangem.blockchain.common.address.AddressType
import com.tangem.domain.common.extensions.amountToCreateAccount
import com.tangem.domain.txhistory.models.TxHistoryItem
import com.tangem.domain.walletmanager.model.Address
import com.tangem.domain.walletmanager.model.CryptoCurrencyAmount
import com.tangem.domain.walletmanager.model.CryptoCurrencyTransaction
import com.tangem.domain.walletmanager.model.UpdateWalletManagerResult
import timber.log.Timber
import java.math.BigDecimal
import com.tangem.blockchain.common.address.Address as SdkAddress

internal class UpdateWalletManagerResultFactory {

    fun getResult(walletManager: WalletManager): UpdateWalletManagerResult.Verified {
        val wallet = walletManager.wallet
        val addresses = getAvailableAddresses(wallet.addresses)

        return UpdateWalletManagerResult.Verified(
            selectedAddress = wallet.address,
            addresses = addresses,
            currenciesAmounts = getTokensAmounts(wallet.amounts.values.toSet()),
            currentTransactions = getCurrentTransactions(addresses, wallet.recentTransactions.toSet()),
        )
    }

    fun getDemoResult(walletManager: WalletManager, demoAmount: Amount): UpdateWalletManagerResult.Verified {
        val wallet = walletManager.wallet
        val addresses = getAvailableAddresses(wallet.addresses)

        return UpdateWalletManagerResult.Verified(
            selectedAddress = wallet.address,
            addresses = addresses,
            currenciesAmounts = getDemoTokensAmounts(demoAmount, walletManager.cardTokens),
            currentTransactions = getCurrentTransactions(addresses, wallet.recentTransactions.toSet()),
        )
    }

    fun getNoAccountResult(walletManager: WalletManager, customMessage: String): UpdateWalletManagerResult {
        val wallet = walletManager.wallet
        val blockchain = wallet.blockchain
        val firstWalletToken = wallet.getTokens().firstOrNull()
        val amountToCreateAccount = blockchain.amountToCreateAccount(firstWalletToken)

        return if (amountToCreateAccount == null) {
            Timber.w("Unable to get required amount to create account for: $blockchain")
            UpdateWalletManagerResult.Unreachable(
                selectedAddress = wallet.address,
                addresses = getAvailableAddresses(wallet.addresses),
            )
        } else {
            UpdateWalletManagerResult.NoAccount(
                selectedAddress = wallet.address,
                addresses = getAvailableAddresses(wallet.addresses),
                amountToCreateAccount = amountToCreateAccount,
                errorMessage = customMessage,
            )
        }
    }

    fun getUnreachableResult(walletManager: WalletManager): UpdateWalletManagerResult {
        val wallet = walletManager.wallet

        return UpdateWalletManagerResult.Unreachable(
            selectedAddress = wallet.address,
            addresses = getAvailableAddresses(wallet.addresses),
        )
    }

    private fun getTokensAmounts(amounts: Set<Amount>): Set<CryptoCurrencyAmount> {
        return amounts.mapNotNullTo(hashSetOf(), ::createCurrencyAmount)
    }

    private fun getDemoTokensAmounts(demoAmount: Amount, tokens: Set<Token>): Set<CryptoCurrencyAmount> {
        val amountValue = demoAmount.value ?: BigDecimal.ZERO
        val demoAmounts = hashSetOf<CryptoCurrencyAmount>(CryptoCurrencyAmount.Coin(amountValue))

        return tokens.mapTo(demoAmounts) { token ->
            CryptoCurrencyAmount.Token(token.id, token.contractAddress, amountValue)
        }
    }

    private fun getCurrentTransactions(
        walletAddresses: Set<Address>,
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
        walletAddresses: Set<Address>,
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

    private fun createTxHistoryItem(walletAddresses: Set<Address>, data: TransactionData): TxHistoryItem? {
        val hash = data.hash ?: return null
        val millis = data.date?.timeInMillis ?: return null
        val amount = getTransactionAmountValue(data.amount) ?: return null
        val isOutgoing = data.sourceAddress in walletAddresses.map { it.value }

        return TxHistoryItem(
            txHash = hash,
            timestampInMillis = millis,
            isOutgoing = isOutgoing,
            destinationType = TxHistoryItem.DestinationType.Single(
                TxHistoryItem.AddressType.User(data.destinationAddress),
            ),
            sourceType = TxHistoryItem.SourceType.Single(data.sourceAddress),
            interactionAddressType = TxHistoryItem.InteractionAddressType.User(
                if (isOutgoing) data.destinationAddress else data.sourceAddress,
            ),
            status = when (data.status) {
                TransactionStatus.Confirmed -> TxHistoryItem.TransactionStatus.Confirmed
                TransactionStatus.Unconfirmed -> TxHistoryItem.TransactionStatus.Unconfirmed
            },
            type = TxHistoryItem.TransactionType.Transfer,
            amount = amount,
        )
    }

    private fun getAvailableAddresses(addresses: Set<SdkAddress>): Set<Address> {
        return addresses.mapTo(hashSetOf()) { sdkAddress ->
            Address(
                value = sdkAddress.value,
                type = when (sdkAddress.type) {
                    AddressType.Default -> Address.Type.Primary
                    AddressType.Legacy -> Address.Type.Secondary
                },
            )
        }
    }

    private fun getCurrencyAmountValue(amount: Amount): BigDecimal? {
        val value = amount.value

        if (value == null) {
            Timber.w("Currency amount must not be null: ${amount.currencySymbol}")
        }

        return value
    }

    private fun getTransactionAmountValue(amount: Amount): BigDecimal? {
        val value = amount.value

        if (value == null) {
            Timber.w("Transaction amount must not be null: ${amount.currencySymbol}")
        }

        return value
    }
}