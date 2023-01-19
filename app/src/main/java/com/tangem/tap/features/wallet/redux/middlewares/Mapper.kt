package com.tangem.tap.features.wallet.redux.middlewares

import com.tangem.blockchain.common.AmountType
import com.tangem.common.extensions.isZero
import com.tangem.tap.common.extensions.stripZeroPlainString
import com.tangem.tap.common.extensions.toFiatRateString
import com.tangem.tap.common.extensions.toFiatValue
import com.tangem.tap.common.extensions.toFormattedCurrencyString
import com.tangem.tap.common.extensions.toFormattedFiatValue
import com.tangem.tap.domain.model.TotalFiatBalance
import com.tangem.tap.domain.model.WalletDataModel
import com.tangem.tap.domain.model.WalletStoreModel
import com.tangem.tap.features.wallet.models.Currency
import com.tangem.tap.features.wallet.models.TotalBalance
import com.tangem.tap.features.wallet.models.WalletRent
import com.tangem.tap.features.wallet.redux.ProgressState
import com.tangem.tap.features.wallet.redux.WalletAddresses
import com.tangem.tap.features.wallet.redux.WalletData
import com.tangem.tap.features.wallet.redux.WalletMainButton
import com.tangem.tap.features.wallet.redux.WalletStore
import com.tangem.tap.features.wallet.ui.BalanceStatus
import com.tangem.tap.features.wallet.ui.BalanceWidgetData
import com.tangem.tap.features.wallet.ui.TokenData
import com.tangem.tap.store
import java.math.BigDecimal

internal fun List<WalletStoreModel>.mapToReduxModels(
    isMultiWalletAllowed: Boolean,
): List<WalletStore> {
    return this.map { walletStoreModel ->
        walletStoreModel.mapToReduxModel(isMultiWalletAllowed)
    }
}

internal fun TotalFiatBalance.mapToReduxModel(): TotalBalance {
    return TotalBalance(
        state = when (this) {
            is TotalFiatBalance.Loading -> ProgressState.Loading
            is TotalFiatBalance.Error -> ProgressState.Error
            is TotalFiatBalance.Loaded -> ProgressState.Done
        },
        fiatAmount = amount,
        fiatCurrency = store.state.globalState.appCurrency,
    )
}

internal fun WalletStoreModel.mapToReduxModel(
    isMultiWalletAllowed: Boolean,
): WalletStore {
    return WalletStore(
        walletManager = walletManager,
        blockchainNetwork = blockchainNetwork,
        walletsData = walletsData.mapToReduxModel(isMultiWalletAllowed, walletRent),
    )
}

@Suppress("LongMethod", "ComplexMethod")
private fun List<WalletDataModel>.mapToReduxModel(
    isMultiWalletAllowed: Boolean,
    walletRent: WalletStoreModel.WalletRent?,
): List<WalletData> {
    return this.map { walletDataModel ->
        with(walletDataModel) {
            val amount = status.amount
            val amountFormatted = amount.toFormattedCurrencyString(
                decimals = currency.decimals,
                currency = currency.currencySymbol,
            )
            val appCurrency = store.state.globalState.appCurrency
            val fiatAmount = fiatRate?.let { status.amount.toFiatValue(it) }
            val fiatAmountFormatted = fiatAmount
                ?.takeIf { !status.isErrorStatus }
                ?.toFormattedFiatValue(appCurrency.symbol)
            val fiatRateFormatted = fiatRate?.toFiatRateString(appCurrency.symbol)
            val blockchainAmountValue = getBlockchainAmount()

            WalletData(
                currency = currency,
                walletAddresses = walletAddresses.getOrNull(0)?.let { selectedAddress ->
                    WalletAddresses(
                        selectedAddress = selectedAddress,
                        list = walletAddresses,
                    )
                },
                existentialDepositString = existentialDeposit?.toPlainString(),
                fiatRate = fiatRate,
                fiatRateString = fiatRateFormatted,
                pendingTransactions = status.pendingTransactions,
                mainButton = WalletMainButton.SendButton(
                    enabled = !blockchainAmountValue.isZero() &&
                        !status.amount.isZero() &&
                        status.pendingTransactions.isEmpty(),
                ),
                walletRent = walletRent?.let {
                    WalletRent(
                        minRentValue = "${it.rent.stripZeroPlainString()} ${currency.blockchain.currency}",
                        rentExemptValue = "${it.exemptionAmount.stripZeroPlainString()} " +
                            currency.blockchain.currency,
                    )
                },
                currencyData = BalanceWidgetData(
                    status = when (status) {
                        is WalletDataModel.Loading -> BalanceStatus.Loading
                        is WalletDataModel.NoAccount -> BalanceStatus.NoAccount
                        is WalletDataModel.SameCurrencyTransactionInProgress ->
                            BalanceStatus.SameCurrencyTransactionInProgress
                        is WalletDataModel.TransactionInProgress -> BalanceStatus.TransactionInProgress
                        is WalletDataModel.Unreachable -> BalanceStatus.Unreachable
                        is WalletDataModel.MissedDerivation -> BalanceStatus.MissedDerivation
                        is WalletDataModel.VerifiedOnline -> BalanceStatus.VerifiedOnline
                    },
                    currency = currency.currencyName,
                    currencySymbol = currency.currencySymbol,
                    blockchainAmount = blockchainAmountValue,
                    amount = amount,
                    amountFormatted = amountFormatted,
                    fiatAmount = fiatAmount,
                    fiatAmountFormatted = fiatAmountFormatted,
                    token = when {
                        !isMultiWalletAllowed && currency is Currency.Token -> {
                            TokenData(
                                amount = amount,
                                amountFormatted = amountFormatted,
                                fiatAmount = fiatAmount,
                                fiatAmountFormatted = fiatAmountFormatted,
                                tokenSymbol = currency.currencySymbol,
                                fiatRate = fiatRate,
                                fiatRateString = fiatRateFormatted,
                            )
                        }
                        else -> null
                    },
                    amountToCreateAccount = (status as? WalletDataModel.NoAccount)
                        ?.amountToCreateAccount
                        ?.toString(),
                    errorMessage = status.errorMessage,
                ),
            )
        }
    }
}

private fun WalletDataModel.getBlockchainAmount(): BigDecimal {
    val walletStore = store.state.walletState.getWalletStore(currency) ?: return BigDecimal.ZERO
    return walletStore.walletManager?.wallet?.amounts?.get(AmountType.Coin)?.value ?: BigDecimal.ZERO
}
