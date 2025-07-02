package com.tangem.domain.walletmanager.utils

import com.tangem.blockchain.common.*
import com.tangem.blockchainsdk.models.UpdateWalletManagerResult
import com.tangem.blockchainsdk.models.UpdateWalletManagerResult.*
import com.tangem.blockchainsdk.utils.amountToCreateAccount
import com.tangem.domain.models.currency.CryptoCurrency
import timber.log.Timber
import java.math.BigDecimal
import com.tangem.blockchain.common.address.Address as SdkAddress

/** Factory for creating [UpdateWalletManagerResult] */
internal class UpdateWalletManagerResultFactory {

    /** Get [Verified] result for [walletManager] */
    fun getResult(walletManager: WalletManager): Verified {
        val wallet = walletManager.wallet
        val addresses = getAvailableAddresses(wallet.addresses)
        val feePaidCurrency = wallet.blockchain.feePaidCurrency()
        val txHistoryItemConverter = TransactionDataToTxHistoryItemConverter(addresses, feePaidCurrency)

        return Verified(
            selectedAddress = wallet.address,
            addresses = addresses,
            currenciesAmounts = getTokensAmounts(wallet.amounts.values.toSet()),
            currentTransactions = getCurrentTransactions(txHistoryItemConverter, wallet.recentTransactions.toSet()),
        )
    }

    /**
     * Get demo [Verified] result
     *
     * @param walletManager wallet manager
     * @param demoAmount    amount that will be used for demo result
     */
    fun getDemoResult(walletManager: WalletManager, demoAmount: Amount): Verified {
        val wallet = walletManager.wallet
        val addresses = getAvailableAddresses(wallet.addresses)
        val feePaidCurrency = wallet.blockchain.feePaidCurrency()
        val txHistoryItemConverter = TransactionDataToTxHistoryItemConverter(addresses, feePaidCurrency)

        return Verified(
            selectedAddress = wallet.address,
            addresses = addresses,
            currenciesAmounts = getDemoTokensAmounts(demoAmount, walletManager.cardTokens),
            currentTransactions = getCurrentTransactions(txHistoryItemConverter, wallet.recentTransactions.toSet()),
        )
    }

    /**
     * Get [NoAccount] result.
     * If unable to get required amount for creating account, [Unreachable] result will be returned.
     *
     * @param walletManager         wallet manager
     * @param customMessage         custom error message
     * @param amountToCreateAccount amount to create account
     */
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
            Unreachable(
                selectedAddress = wallet.address,
                addresses = getAvailableAddresses(wallet.addresses),
            )
        } else {
            NoAccount(
                selectedAddress = wallet.address,
                addresses = getAvailableAddresses(wallet.addresses),
                amountToCreateAccount = amount,
                errorMessage = customMessage,
            )
        }
    }

    /** Get [Unreachable] result for [walletManager] */
    fun getUnreachableResult(walletManager: WalletManager): Unreachable {
        val wallet = walletManager.wallet

        return Unreachable(
            selectedAddress = wallet.address,
            addresses = getAvailableAddresses(wallet.addresses),
        )
    }

    private fun getAvailableAddresses(addresses: Set<SdkAddress>): Set<Address> {
        return SdkAddressToAddressConverter.convertList(addresses).toSet()
    }

    private fun getTokensAmounts(amounts: Set<Amount>): Set<CryptoCurrencyAmount> {
        return amounts.mapNotNullTo(hashSetOf(), ::createCurrencyAmount)
    }

    private fun createCurrencyAmount(amount: Amount): CryptoCurrencyAmount? {
        return when (val type = amount.type) {
            is AmountType.Token -> {
                val value = getCurrencyAmountValue(amount) ?: return null

                CryptoCurrencyAmount.Token(
                    currencyRawId = type.token.id?.let(CryptoCurrency::RawID),
                    contractAddress = type.token.contractAddress,
                    value = value,
                )
            }
            is AmountType.Coin -> {
                val value = getCurrencyAmountValue(amount) ?: return null

                CryptoCurrencyAmount.Coin(value = value)
            }
            is AmountType.FeeResource,
            is AmountType.Reserve,
            -> null
        }
    }

    private fun getCurrencyAmountValue(amount: Amount): BigDecimal? {
        val value = amount.value

        if (value == null) {
            Timber.w("Currency amount must not be null: ${amount.currencySymbol}")
        }

        return value
    }

    private fun getDemoTokensAmounts(demoAmount: Amount, tokens: Set<Token>): Set<CryptoCurrencyAmount> {
        val amountValue = demoAmount.value ?: BigDecimal.ZERO
        val demoAmounts = hashSetOf<CryptoCurrencyAmount>(CryptoCurrencyAmount.Coin(amountValue))

        return tokens.mapTo(demoAmounts) { token ->
            CryptoCurrencyAmount.Token(
                currencyRawId = token.id?.let(CryptoCurrency::RawID),
                contractAddress = token.contractAddress,
                value = amountValue,
            )
        }
    }

    private fun getCurrentTransactions(
        txHistoryItemConverter: TransactionDataToTxHistoryItemConverter,
        recentTransactions: Set<TransactionData.Uncompiled>,
    ): Set<CryptoCurrencyTransaction> {
        val unconfirmedTransactions = recentTransactions.filter { it.status == TransactionStatus.Unconfirmed }

        return unconfirmedTransactions.mapNotNullTo(hashSetOf()) {
            createCurrencyTransaction(
                txHistoryItemConverter = txHistoryItemConverter,
                data = it,
            )
        }
    }

    private fun createCurrencyTransaction(
        txHistoryItemConverter: TransactionDataToTxHistoryItemConverter,
        data: TransactionData.Uncompiled,
    ): CryptoCurrencyTransaction? {
        return when (val type = data.amount.type) {
            is AmountType.Coin -> {
                val txHistoryItem = txHistoryItemConverter.convert(data) ?: return null

                CryptoCurrencyTransaction.Coin(txInfo = txHistoryItem)
            }
            is AmountType.Token -> {
                val txHistoryItem = txHistoryItemConverter.convert(data) ?: return null

                CryptoCurrencyTransaction.Token(
                    tokenId = type.token.id,
                    contractAddress = type.token.contractAddress,
                    txInfo = txHistoryItem,
                )
            }
            is AmountType.FeeResource,
            is AmountType.Reserve,
            -> null
        }
    }
}