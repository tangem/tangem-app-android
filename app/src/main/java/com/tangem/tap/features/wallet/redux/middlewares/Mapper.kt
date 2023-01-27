package com.tangem.tap.features.wallet.redux.middlewares

import com.tangem.blockchain.common.Blockchain
import com.tangem.common.extensions.isZero
import com.tangem.tap.common.extensions.stripZeroPlainString
import com.tangem.tap.common.extensions.toFiatRateString
import com.tangem.tap.common.extensions.toFiatValue
import com.tangem.tap.common.extensions.toFormattedCurrencyString
import com.tangem.tap.common.extensions.toFormattedFiatValue
import com.tangem.tap.domain.model.TotalFiatBalance
import com.tangem.tap.domain.model.WalletDataModel
import com.tangem.tap.domain.model.WalletStoreModel
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
import timber.log.Timber
import java.math.BigDecimal

internal fun List<WalletStoreModel>.mapToReduxModels(): List<WalletStore> {
    return this.map { walletStoreModel ->
        walletStoreModel.mapToReduxModel()
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

internal fun WalletStoreModel.mapToReduxModel(): WalletStore {
    val appCurrencySymbol = store.state.globalState.appCurrency.symbol
    return WalletStore(
        walletManager = walletManager,
        blockchainNetwork = blockchainNetwork,
        walletsData = walletsData.mapToReduxModel(walletRent, appCurrencySymbol),
    )
        .updateTokenModels(blockchain)
        .setupIfHadCardSingleToken(blockchain, walletsData, appCurrencySymbol)
}

private fun List<WalletDataModel>.mapToReduxModel(
    walletRent: WalletStoreModel.WalletRent?,
    appCurrencySymbol: String,
): List<WalletData> {
    return this.map { walletDataModel ->
        with(walletDataModel) {
            val amount = status.amount
            val amountFormatted = amount.toFormattedCurrencyString(
                decimals = currency.decimals,
                currency = currency.currencySymbol,
            )
            val fiatAmount = fiatRate?.let { status.amount.toFiatValue(it) }
            val fiatAmountFormatted = fiatAmount
                ?.takeIf { !status.isErrorStatus }
                ?.toFormattedFiatValue(appCurrencySymbol)
            val fiatRateFormatted = fiatRate?.toFiatRateString(appCurrencySymbol)

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
                    enabled = !status.amount.isZero() && status.pendingTransactions.isEmpty(),
                ),
                walletRent = walletRent?.let {
                    WalletRent(
                        minRentValue = "${it.rent.stripZeroPlainString()} ${currency.blockchain.currency}",
                        rentExemptValue = "${it.exemptionAmount.stripZeroPlainString()} ${currency.blockchain.currency}",
                    )
                },
                currencyData = BalanceWidgetData(
                    status = when (status) {
                        is WalletDataModel.Loading -> BalanceStatus.Loading
                        is WalletDataModel.NoAccount -> BalanceStatus.NoAccount
                        is WalletDataModel.SameCurrencyTransactionInProgress -> BalanceStatus.SameCurrencyTransactionInProgress
                        is WalletDataModel.TransactionInProgress -> BalanceStatus.TransactionInProgress
                        is WalletDataModel.Unreachable -> BalanceStatus.Unreachable
                        is WalletDataModel.MissedDerivation -> BalanceStatus.MissedDerivation
                        is WalletDataModel.VerifiedOnline -> BalanceStatus.VerifiedOnline
                    },
                    currency = currency.currencyName,
                    currencySymbol = currency.currencySymbol,
                    blockchainAmount = BigDecimal.ZERO,
                    amount = amount,
                    amountFormatted = amountFormatted,
                    fiatAmount = fiatAmount,
                    fiatAmountFormatted = fiatAmountFormatted,
                    token = null,
                    amountToCreateAccount = (status as? WalletDataModel.NoAccount)
                        ?.amountToCreateAccount
                        ?.toString(),
                    errorMessage = status.errorMessage,
                ),
            )
        }
    }
}

private fun WalletStore.updateTokenModels(blockchain: Blockchain): WalletStore {
    val foundBlockchains = this.walletsData.filter {
        it.currency.isBlockchain() && it.currency.blockchain == blockchain
    }
    if (foundBlockchains.size != 1) {
        val warningMessage = "Can't update information about Tokens in the WalletData list, because " +
            "of the WalletStore doesn't contains the Blockchain: %s, or contains more than one"
        Timber.w(warningMessage, blockchain.id)
        return this
    }

    val blockchainAmountValue = foundBlockchains.first().currencyData.blockchainAmount ?: BigDecimal.ZERO
    val updatedTokensWalletData = walletsData.filter { it.currency.isToken() }.map {
        it.copy(
            mainButton = when (it.mainButton) {
                is WalletMainButton.SendButton -> {
                    WalletMainButton.SendButton(it.mainButton.enabled && !blockchainAmountValue.isZero())
                }
                is WalletMainButton.CreateWalletButton -> it.mainButton
            },
            currencyData = it.currencyData.copy(
                blockchainAmount = blockchainAmountValue,
            ),
        )
    }
    return updateWallets(updatedTokensWalletData)
}

private fun WalletStore.setupIfHadCardSingleToken(
    blockchain: Blockchain,
    walletsDataModel: List<WalletDataModel>,
    appCurrencySymbol: String,
): WalletStore {
    // Card with single token contains only 2 model - blockchain and token
    if (walletsData.size != 2) return this

    val blockchainWalletData = walletsData.firstOrNull {
        it.currency.isBlockchain() && it.currency.blockchain == blockchain
    }
    val cardSingleTokenWalletData = walletsDataModel.firstOrNull {
        it.currency.isToken() && it.currency.blockchain == blockchain && it.isCardSingleToken
    }
    if (blockchainWalletData == null || cardSingleTokenWalletData == null) return this

    val blockchainWalletDataWithSingleToken = blockchainWalletData.copy(
        currencyData = blockchainWalletData.currencyData.copy(
            token = cardSingleTokenWalletData.toTokenData(appCurrencySymbol),
        ),
    )
    return updateWallets(listOf(blockchainWalletDataWithSingleToken))
}

private fun WalletDataModel.toTokenData(appCurrencySymbol: String): TokenData {
    val amount = status.amount
    val fiatAmount = fiatRate?.let { status.amount.toFiatValue(it) }

    return TokenData(
        amount = amount,
        amountFormatted = amount.toFormattedCurrencyString(
            decimals = currency.decimals,
            currency = currency.currencySymbol,
        ),
        fiatAmount = fiatAmount,
        fiatAmountFormatted = fiatAmount
            ?.takeIf { !status.isErrorStatus }
            ?.toFormattedFiatValue(appCurrencySymbol),
        tokenSymbol = currency.currencySymbol,
        fiatRate = fiatRate,
        fiatRateString = fiatRate?.toFiatRateString(appCurrencySymbol),
    )
}
