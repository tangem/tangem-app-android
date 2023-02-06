package com.tangem.tap.features.wallet.redux

import com.tangem.common.extensions.isZero
import com.tangem.domain.common.extensions.toNetworkId
import com.tangem.feature.swap.domain.SwapInteractor
import com.tangem.tap.features.wallet.models.Currency
import com.tangem.tap.features.wallet.models.PendingTransaction
import com.tangem.tap.features.wallet.models.WalletRent
import com.tangem.tap.features.wallet.models.WalletWarning
import com.tangem.tap.features.wallet.ui.BalanceStatus
import com.tangem.tap.features.wallet.ui.BalanceWidgetData
import com.tangem.tap.network.exchangeServices.CurrencyExchangeManager
import java.math.BigDecimal

data class WalletData(
    val pendingTransactions: List<PendingTransaction> = emptyList(),
    val hashesCountVerified: Boolean? = null,
    val walletAddresses: WalletAddresses? = null,
    val currencyData: BalanceWidgetData = BalanceWidgetData(),
    val updatingWallet: Boolean = false,
    val fiatRateString: String? = null,
    val fiatRate: BigDecimal? = null,
    val mainButton: WalletMainButton = WalletMainButton.SendButton(false),
    val currency: Currency,
    val walletRent: WalletRent? = null,
    val existentialDepositString: String? = null,
) {
    fun isAvailableToBuy(exchangeManager: CurrencyExchangeManager): Boolean {
        return exchangeManager.availableForBuy(currency)
    }

    fun isAvailableToSell(exchangeManager: CurrencyExchangeManager): Boolean {
        return exchangeManager.availableForSell(currency)
    }

    fun isAvailableToSwap(swapInteractor: SwapInteractor, isExchangeFeatureOn: Boolean): Boolean {
        return swapInteractor.isAvailableToSwap(currency.blockchain.toNetworkId()) && isExchangeFeatureOn
    }

    fun getAvailableActions(
        swapInteractor: SwapInteractor,
        exchangeManager: CurrencyExchangeManager,
        isExchangeFeatureOn: Boolean,
    ): Set<CurrencyAction> {
        return setOfNotNull(
            if (isAvailableToBuy(exchangeManager)) CurrencyAction.Buy else null,
            if (isAvailableToSell(exchangeManager)) CurrencyAction.Sell else null,
            if (isAvailableToSwap(swapInteractor, isExchangeFeatureOn)) CurrencyAction.Swap else null,
        )
    }

    fun shouldShowMultipleAddress(): Boolean {
        val listOfAddresses = walletAddresses?.list ?: return false
        return listOfAddresses.size > 1
    }

    fun shouldEnableTokenSendButton(): Boolean = if (blockchainAmountIsEmpty()) {
        false
    } else {
        !tokenAmountIsEmpty()
    }

    fun assembleWarnings(): List<WalletWarning> {
        val walletWarnings = mutableListOf<WalletWarning>()
        assembleNonTypedWarnings(walletWarnings)
        assembleBlockchainWarnings(walletWarnings)
        assembleTokenWarnings(walletWarnings)

        return walletWarnings.sortedBy { it.showingPosition }
    }

    private fun assembleNonTypedWarnings(walletWarnings: MutableList<WalletWarning>) {
        if (currencyData.status == BalanceStatus.SameCurrencyTransactionInProgress) {
            walletWarnings.add(WalletWarning.TransactionInProgress(currency.currencyName))
        }
    }

    private fun assembleBlockchainWarnings(walletWarnings: MutableList<WalletWarning>) {
        if (!currency.isBlockchain()) return

        if (existentialDepositString != null) {
            val warning = WalletWarning.ExistentialDeposit(
                currencyName = currency.currencyName,
                edStringValueWithSymbol = "$existentialDepositString ${currency.currencySymbol}",
            )
            walletWarnings.add(warning)
        }
        if (walletRent != null) {
            walletWarnings.add(WalletWarning.Rent(walletRent))
        }
    }

    private fun assembleTokenWarnings(walletWarnings: MutableList<WalletWarning>) {
        with(currency) {
            if (!isToken()) return

            if (blockchainAmountIsEmpty() && !tokenAmountIsEmpty()) {
                walletWarnings.add(
                    WalletWarning.BalanceNotEnoughForFee(
                        currencyName = currencyName,
                        blockchainFullName = blockchain.fullName,
                        blockchainSymbol = blockchain.currency,
                    ),
                )
            }
        }
    }

    private fun blockchainAmountIsEmpty(): Boolean = currencyData.blockchainAmount?.isZero() == true

    private fun tokenAmountIsEmpty(): Boolean = currencyData.amount?.isZero() == true
}

enum class CurrencyAction {
    Buy, Sell, Swap
}
