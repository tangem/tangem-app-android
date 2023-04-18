package com.tangem.tap.features.wallet.ui.utils

import com.tangem.blockchain.common.Blockchain
import com.tangem.common.extensions.isZero
import com.tangem.domain.common.extensions.toNetworkId
import com.tangem.feature.swap.api.SwapFeatureToggleManager
import com.tangem.feature.swap.domain.SwapInteractor
import com.tangem.tap.common.entities.FiatCurrency
import com.tangem.tap.common.extensions.toFiatRateString
import com.tangem.tap.common.extensions.toFiatValue
import com.tangem.tap.common.extensions.toFormattedCurrencyString
import com.tangem.tap.common.extensions.toFormattedFiatValue
import com.tangem.tap.domain.model.WalletDataModel
import com.tangem.tap.domain.model.WalletStoreModel
import com.tangem.tap.features.wallet.models.WalletWarning
import com.tangem.tap.features.wallet.redux.WalletMainButton
import com.tangem.tap.features.wallet.redux.utils.UNKNOWN_AMOUNT_SIGN
import com.tangem.tap.network.exchangeServices.CurrencyExchangeManager
import java.math.BigDecimal

internal val WalletDataModel.mainButton: WalletMainButton
    get() = WalletMainButton.SendButton(
        enabled = !status.amount.isZero() && status.pendingTransactions.isEmpty(),
    )

internal fun WalletDataModel.getFormattedAmount(): String {
    return status.amount.toFormattedCurrencyString(
        decimals = currency.decimals,
        currency = currency.currencySymbol,
    )
}

internal fun WalletDataModel.getFormattedFiatAmount(
    fiatCurrency: FiatCurrency,
    unknownAmountSign: String = UNKNOWN_AMOUNT_SIGN,
): String {
    return this.fiatRate?.let { status.amount.toFiatValue(it) }
        ?.takeIf { !status.isErrorStatus }
        ?.toFormattedFiatValue(fiatCurrencyName = fiatCurrency.symbol, fiatCode = fiatCurrency.code)
        ?: unknownAmountSign
}

internal fun WalletDataModel.getFormattedFiatRate(fiatCurrency: FiatCurrency, noRateValue: String): String {
    return fiatRate?.toFiatRateString(fiatCurrency.symbol, fiatCurrency.code)
        ?: noRateValue
}

internal fun WalletDataModel.isAvailableToBuy(exchangeManager: CurrencyExchangeManager): Boolean {
    return exchangeManager.availableForBuy(currency)
}

internal fun WalletDataModel.isAvailableToSell(exchangeManager: CurrencyExchangeManager): Boolean {
    return exchangeManager.availableForSell(currency)
}

internal fun WalletDataModel.isAvailableToSwap(
    swapFeatureToggleManager: SwapFeatureToggleManager,
    swapInteractor: SwapInteractor,
): Boolean {
    if (currency.blockchain.id == Blockchain.Optimism.id && !swapFeatureToggleManager.isOptimismSwapEnabled) {
        return false
    }
    return swapInteractor.isAvailableToSwap(currency.blockchain.toNetworkId()) &&
        !currency.isCustomCurrency(null)
}

internal fun WalletDataModel.getAvailableActions(
    swapInteractor: SwapInteractor,
    exchangeManager: CurrencyExchangeManager,
    swapFeatureToggleManager: SwapFeatureToggleManager,
): Set<CurrencyAction> {
    return setOfNotNull(
        if (isAvailableToBuy(exchangeManager)) CurrencyAction.Buy else null,
        if (isAvailableToSell(exchangeManager)) CurrencyAction.Sell else null,
        if (isAvailableToSwap(swapFeatureToggleManager, swapInteractor)) CurrencyAction.Swap else null,
    )
}

internal fun WalletDataModel.shouldShowMultipleAddress(): Boolean {
    val listOfAddresses = walletAddresses?.list.orEmpty()
    return listOfAddresses.size > 1
}

internal fun WalletDataModel.assembleWarnings(
    blockchainAmount: BigDecimal,
    blockchainWalletRent: WalletStoreModel.WalletRent?,
): List<WalletWarning> {
    val walletWarnings = mutableListOf<WalletWarning>()
    assembleNonTypedWarnings(walletWarnings, blockchainWalletRent)
    assembleBlockchainWarnings(walletWarnings)
    assembleTokenWarnings(walletWarnings, blockchainAmount)

    return walletWarnings.sortedBy { it.showingPosition }
}

private fun WalletDataModel.assembleNonTypedWarnings(
    walletWarnings: MutableList<WalletWarning>,
    walletRent: WalletStoreModel.WalletRent?,
) {
    if (this.status is WalletDataModel.SameCurrencyTransactionInProgress) {
        walletWarnings.add(WalletWarning.TransactionInProgress(currency.currencyName))
    }
    if (walletRent != null) {
        walletWarnings.add(WalletWarning.Rent(walletRent))
    }
}

private fun WalletDataModel.assembleBlockchainWarnings(walletWarnings: MutableList<WalletWarning>) {
    with(currency) {
        if (!isBlockchain()) return

        if (existentialDeposit != null) {
            val warning = WalletWarning.ExistentialDeposit(
                currencyName = currencyName,
                edStringValueWithSymbol = "${existentialDeposit.toPlainString()} $currencySymbol",
            )
            walletWarnings.add(warning)
        }
    }
}

private fun WalletDataModel.assembleTokenWarnings(
    walletWarnings: MutableList<WalletWarning>,
    blockchainAmount: BigDecimal,
) {
    if (!currency.isToken()) return

    if (!this.isEmptyAmount && blockchainAmount.isZero()) {
        walletWarnings.add(
            WalletWarning.BalanceNotEnoughForFee(
                currencyName = currency.currencyName,
                blockchainFullName = currency.blockchain.fullName,
                blockchainSymbol = currency.blockchain.currency,
            ),
        )
    }
}

private val WalletDataModel.isEmptyAmount: Boolean
    get() = this.status.amount.isZero()

enum class CurrencyAction {
    Buy, Sell, Swap
}
