package com.tangem.data.walletmanager

import com.tangem.blockchain.common.*
import com.tangem.blockchain.common.address.Address
import com.tangem.blockchainsdk.models.UpdateWalletManagerResult
import com.tangem.blockchainsdk.utils.amountToCreateAccount
import com.tangem.data.walletmanager.utils.SdkAddressToAddressConverter
import com.tangem.data.walletmanager.utils.TransactionDataToTxHistoryItemConverter
import com.tangem.domain.models.currency.CryptoCurrency
import com.tangem.domain.models.yield.supply.YieldSupplyStatus
import timber.log.Timber
import java.math.BigDecimal

/** Factory for creating [UpdateWalletManagerResult] */
internal class UpdateWalletManagerResultFactory {

    /** Get [UpdateWalletManagerResult.Verified] result for [walletManager] */
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

    /**
     * Get demo [UpdateWalletManagerResult.Verified] result
     *
     * @param walletManager wallet manager
     * @param demoAmount    amount that will be used for demo result
     */
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

    /**
     * Get [UpdateWalletManagerResult.NoAccount] result.
     * If unable to get required amount for creating account, [UpdateWalletManagerResult.Unreachable] result will be returned.
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

    /** Get [UpdateWalletManagerResult.Unreachable] result for [walletManager] */
    fun getUnreachableResult(walletManager: WalletManager): UpdateWalletManagerResult.Unreachable {
        val wallet = walletManager.wallet

        return UpdateWalletManagerResult.Unreachable(
            selectedAddress = wallet.address,
            addresses = getAvailableAddresses(wallet.addresses),
        )
    }

    private fun getAvailableAddresses(addresses: Set<Address>): Set<UpdateWalletManagerResult.Address> {
        return SdkAddressToAddressConverter.convertList(addresses).toSet()
    }

    private fun getTokensAmounts(amounts: Set<Amount>): Set<UpdateWalletManagerResult.CryptoCurrencyAmount> {
        return amounts.mapNotNullTo(hashSetOf(), ::createCurrencyAmount)
    }

    private fun createCurrencyAmount(amount: Amount): UpdateWalletManagerResult.CryptoCurrencyAmount? {
        return when (val type = amount.type) {
            is AmountType.Token -> {
                val value = getCurrencyAmountValue(amount) ?: return null

                UpdateWalletManagerResult.CryptoCurrencyAmount.Token.BasicToken(
                    currencyRawId = type.token.id?.let(CryptoCurrency::RawID),
                    contractAddress = type.token.contractAddress,
                    value = value,
                )
            }
            is AmountType.Coin -> {
                val value = getCurrencyAmountValue(amount) ?: return null

                UpdateWalletManagerResult.CryptoCurrencyAmount.Coin(value = value)
            }
            is AmountType.TokenYieldSupply -> {
                val value = getCurrencyAmountValue(amount) ?: return null

                UpdateWalletManagerResult.CryptoCurrencyAmount.Token.YieldSupplyToken(
                    value = value,
                    currencyRawId = type.token.id?.let(CryptoCurrency::RawID),
                    contractAddress = type.token.contractAddress,
                    yieldSupplyStatus = YieldSupplyStatus(
                        isActive = type.isActive,
                        isInitialized = type.isInitialized,
                        isAllowedToSpend = type.isAllowedToSpend,
                    ),
                )
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

    private fun getDemoTokensAmounts(
        demoAmount: Amount,
        tokens: Set<Token>,
    ): Set<UpdateWalletManagerResult.CryptoCurrencyAmount> {
        val amountValue = demoAmount.value ?: BigDecimal.ZERO
        val demoAmounts = hashSetOf<UpdateWalletManagerResult.CryptoCurrencyAmount>(
            UpdateWalletManagerResult.CryptoCurrencyAmount.Coin(amountValue),
        )

        return tokens.mapTo(demoAmounts) { token ->
            UpdateWalletManagerResult.CryptoCurrencyAmount.Token.BasicToken(
                currencyRawId = token.id?.let(CryptoCurrency::RawID),
                contractAddress = token.contractAddress,
                value = amountValue,
            )
        }
    }

    private fun getCurrentTransactions(
        txHistoryItemConverter: TransactionDataToTxHistoryItemConverter,
        recentTransactions: Set<TransactionData.Uncompiled>,
    ): Set<UpdateWalletManagerResult.CryptoCurrencyTransaction> {
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
    ): UpdateWalletManagerResult.CryptoCurrencyTransaction? {
        return when (val type = data.amount.type) {
            is AmountType.Coin -> {
                val txHistoryItem = txHistoryItemConverter.convert(data) ?: return null

                UpdateWalletManagerResult.CryptoCurrencyTransaction.Coin(txInfo = txHistoryItem)
            }
            is AmountType.Token -> {
                val txHistoryItem = txHistoryItemConverter.convert(data) ?: return null

                UpdateWalletManagerResult.CryptoCurrencyTransaction.Token(
                    tokenId = type.token.id,
                    contractAddress = type.token.contractAddress,
                    txInfo = txHistoryItem,
                )
            }
            is AmountType.TokenYieldSupply -> {
                val txHistoryItem = txHistoryItemConverter.convert(data) ?: return null

                UpdateWalletManagerResult.CryptoCurrencyTransaction.Token(
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