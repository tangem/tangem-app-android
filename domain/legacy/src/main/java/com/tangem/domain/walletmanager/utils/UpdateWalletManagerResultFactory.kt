package com.tangem.domain.walletmanager.utils

import com.tangem.blockchain.common.*
import com.tangem.blockchainsdk.utils.amountToCreateAccount
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
        val feePaidCurrency = wallet.blockchain.feePaidCurrency()
        val txHistoryItemConverter = TransactionDataToTxHistoryItemConverter(addresses, feePaidCurrency)

        return UpdateWalletManagerResult.Verified(
            selectedAddress = wallet.address,
            addresses = addresses,
            currenciesAmounts = getTokensAmounts(wallet.amounts.values.toSet()),
            currentTransactions = getCurrentTransactions(txHistoryItemConverter, wallet.recentTransactions.toSet()),
        )
    }

    fun getDemoResult(walletManager: WalletManager, demoAmount: Amount): UpdateWalletManagerResult.Verified {
        val wallet = walletManager.wallet
        val addresses = getAvailableAddresses(wallet.addresses)
        val feePaidCurrency = wallet.blockchain.feePaidCurrency()
        val txHistoryItemConverter = TransactionDataToTxHistoryItemConverter(addresses, feePaidCurrency)

        return UpdateWalletManagerResult.Verified(
            selectedAddress = wallet.address,
            addresses = addresses,
            currenciesAmounts = getDemoTokensAmounts(demoAmount, walletManager.cardTokens),
            currentTransactions = getCurrentTransactions(txHistoryItemConverter, wallet.recentTransactions.toSet()),
        )
    }

    fun getNoAccountResult(
        walletManager: WalletManager,
        customMessage: String,
        amountToCreateAccount: BigDecimal?,
    ): UpdateWalletManagerResult {
        val wallet = walletManager.wallet
        val blockchain = wallet.blockchain
        val firstWalletToken = wallet.getTokens().firstOrNull()
        val amount = amountToCreateAccount ?: blockchain.amountToCreateAccount(walletManager, firstWalletToken)

        return if (amount == null) {
            Timber.w("Unable to get required amount to create account for: $blockchain")
            UpdateWalletManagerResult.Unreachable(
                selectedAddress = wallet.address,
                addresses = getAvailableAddresses(wallet.addresses),
            )
        } else {
            UpdateWalletManagerResult.NoAccount(
                selectedAddress = wallet.address,
                addresses = getAvailableAddresses(wallet.addresses),
                amountToCreateAccount = amount,
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
        txHistoryItemConverter: TransactionDataToTxHistoryItemConverter,
        recentTransactions: Set<TransactionData.Uncompiled>,
    ): Set<CryptoCurrencyTransaction> {
        val unconfirmedTransactions = recentTransactions.filter {
            it.status == TransactionStatus.Unconfirmed
        }

        return unconfirmedTransactions.mapNotNullTo(hashSetOf()) {
            createCurrencyTransaction(
                txHistoryItemConverter = txHistoryItemConverter,
                data = it,
            )
        }
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
            is AmountType.FeeResource,
            AmountType.Reserve,
            -> null
        }
    }

    private fun createCurrencyTransaction(
        txHistoryItemConverter: TransactionDataToTxHistoryItemConverter,
        data: TransactionData.Uncompiled,
    ): CryptoCurrencyTransaction? {
        return when (val type = data.amount.type) {
            is AmountType.Coin -> {
                val txHistoryItem = txHistoryItemConverter.convert(data) ?: return null
                CryptoCurrencyTransaction.Coin(txHistoryItem)
            }
            is AmountType.Token -> {
                val txHistoryItem = txHistoryItemConverter.convert(data) ?: return null
                CryptoCurrencyTransaction.Token(
                    tokenId = type.token.id,
                    tokenContractAddress = type.token.contractAddress,
                    txHistoryItem = txHistoryItem,
                )
            }
            is AmountType.FeeResource,
            AmountType.Reserve,
            -> null
        }
    }

    private fun getAvailableAddresses(addresses: Set<SdkAddress>): Set<Address> {
        return SdkAddressToAddressConverter.convertList(addresses).toSet()
    }

    private fun getCurrencyAmountValue(amount: Amount): BigDecimal? {
        val value = amount.value

        if (value == null) {
            Timber.w("Currency amount must not be null: ${amount.currencySymbol}")
        }

        return value
    }
}