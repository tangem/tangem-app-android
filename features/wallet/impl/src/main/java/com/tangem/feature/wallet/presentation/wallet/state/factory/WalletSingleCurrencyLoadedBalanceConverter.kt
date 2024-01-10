package com.tangem.feature.wallet.presentation.wallet.state.factory

import arrow.core.Either
import com.tangem.core.ui.components.marketprice.MarketPriceBlockState
import com.tangem.core.ui.components.marketprice.PriceChangeState
import com.tangem.core.ui.components.marketprice.PriceChangeType
import com.tangem.core.ui.utils.BigDecimalFormatter
import com.tangem.domain.appcurrency.model.AppCurrency
import com.tangem.domain.tokens.error.CurrencyStatusError
import com.tangem.domain.tokens.model.CryptoCurrencyStatus
import com.tangem.domain.wallets.models.UserWallet
import com.tangem.feature.wallet.presentation.wallet.domain.WalletAdditionalInfoFactory
import com.tangem.feature.wallet.presentation.wallet.domain.getCardsCount
import com.tangem.feature.wallet.presentation.wallet.state.WalletMultiCurrencyState
import com.tangem.feature.wallet.presentation.wallet.state.WalletSingleCurrencyState
import com.tangem.feature.wallet.presentation.wallet.state.WalletState
import com.tangem.feature.wallet.presentation.wallet.state.components.WalletCardState
import com.tangem.feature.wallet.presentation.wallet.state.components.WalletsListConfig
import com.tangem.feature.wallet.presentation.wallet.utils.CurrencyStatusErrorConverter
import com.tangem.utils.Provider
import com.tangem.utils.converter.Converter
import kotlinx.collections.immutable.toPersistentList
import java.math.BigDecimal

internal class WalletSingleCurrencyLoadedBalanceConverter(
    private val currentStateProvider: Provider<WalletState>,
    private val appCurrencyProvider: Provider<AppCurrency>,
    private val currentWalletProvider: Provider<UserWallet>,
    private val currencyStatusErrorConverter: CurrencyStatusErrorConverter,
) : Converter<Either<CurrencyStatusError, CryptoCurrencyStatus>, WalletState> {

    override fun convert(value: Either<CurrencyStatusError, CryptoCurrencyStatus>): WalletState {
        return value.fold(
            ifLeft = currencyStatusErrorConverter::convert,
            ifRight = ::convertContent,
        )
    }

    private fun convertContent(status: CryptoCurrencyStatus): WalletState {
        return when (val state = currentStateProvider()) {
            is WalletSingleCurrencyState.Content -> {
                val currencyName = state.marketPriceBlockState.currencySymbol

                state.copy(
                    walletsListConfig = getUpdatedSelectedWallet(status = status.value, state = state),
                    marketPriceBlockState = getMarketPriceState(status = status.value, currencySymbol = currencyName),
                )
            }
            is WalletMultiCurrencyState.Content,
            is WalletMultiCurrencyState.Locked,
            is WalletSingleCurrencyState.Locked,
            is WalletState.Initial,
            -> state
        }
    }

    private fun getMarketPriceState(
        status: CryptoCurrencyStatus.Status,
        currencySymbol: String,
    ): MarketPriceBlockState {
        return when (status) {
            is CryptoCurrencyStatus.Loaded,
            is CryptoCurrencyStatus.NoAmount,
            -> status.toContentConfig(currencySymbol)
            is CryptoCurrencyStatus.NoAccount -> {
                if (status.fiatRate == null) {
                    MarketPriceBlockState.Error(currencySymbol)
                } else {
                    status.toContentConfig(currencySymbol)
                }
            }
            is CryptoCurrencyStatus.Loading -> MarketPriceBlockState.Loading(currencySymbol)
            is CryptoCurrencyStatus.Custom,
            is CryptoCurrencyStatus.MissedDerivation,
            is CryptoCurrencyStatus.Unreachable,
            is CryptoCurrencyStatus.UnreachableWithoutAddresses,
            is CryptoCurrencyStatus.NoQuote,
            -> MarketPriceBlockState.Error(currencySymbol)
        }
    }

    private fun getUpdatedSelectedWallet(
        status: CryptoCurrencyStatus.Status,
        state: WalletSingleCurrencyState,
    ): WalletsListConfig {
        val selectedWallet = state.walletsListConfig.wallets[state.walletsListConfig.selectedWalletIndex]

        val updatedWallet = when (status) {
            is CryptoCurrencyStatus.NoQuote,
            is CryptoCurrencyStatus.Loaded,
            is CryptoCurrencyStatus.NoAccount,
            is CryptoCurrencyStatus.NoAmount,
            -> {
                WalletCardState.Content(
                    id = selectedWallet.id,
                    title = selectedWallet.title,
                    additionalInfo = WalletAdditionalInfoFactory.resolve(
                        wallet = currentWalletProvider(),
                        currencyAmount = status.amount,
                    ),
                    imageResId = selectedWallet.imageResId,
                    onRenameClick = selectedWallet.onRenameClick,
                    onDeleteClick = selectedWallet.onDeleteClick,
                    balance = formatFiatAmount(status = status, appCurrency = appCurrencyProvider()),
                    cardCount = currentWalletProvider().getCardsCount(),
                )
            }
            is CryptoCurrencyStatus.Loading -> {
                WalletCardState.Loading(
                    id = selectedWallet.id,
                    title = selectedWallet.title,
                    imageResId = selectedWallet.imageResId,
                    onRenameClick = selectedWallet.onRenameClick,
                    onDeleteClick = selectedWallet.onDeleteClick,
                )
            }
            is CryptoCurrencyStatus.MissedDerivation,
            is CryptoCurrencyStatus.Custom,
            is CryptoCurrencyStatus.Unreachable,
            is CryptoCurrencyStatus.UnreachableWithoutAddresses,
            -> {
                WalletCardState.Error(
                    id = selectedWallet.id,
                    title = selectedWallet.title,
                    imageResId = selectedWallet.imageResId,
                    onRenameClick = selectedWallet.onRenameClick,
                    onDeleteClick = selectedWallet.onDeleteClick,
                )
            }
        }

        return state.walletsListConfig.copy(
            wallets = state.walletsListConfig.wallets.toPersistentList()
                .set(index = state.walletsListConfig.selectedWalletIndex, element = updatedWallet),
        )
    }

    private fun CryptoCurrencyStatus.Status.toContentConfig(currencySymbol: String): MarketPriceBlockState.Content {
        return MarketPriceBlockState.Content(
            currencySymbol = currencySymbol,
            price = formatPrice(status = this, appCurrency = appCurrencyProvider()),
            priceChangeConfig = PriceChangeState.Content(
                valueInPercent = formatPriceChange(status = this),
                type = getPriceChangeType(status = this),
            ),
        )
    }

    private fun getPriceChangeType(status: CryptoCurrencyStatus.Status): PriceChangeType {
        val priceChange = status.priceChange ?: return PriceChangeType.DOWN

        return if (priceChange > BigDecimal.ZERO) PriceChangeType.UP else PriceChangeType.DOWN
    }

    private fun formatPriceChange(status: CryptoCurrencyStatus.Status): String {
        val priceChange = status.priceChange ?: return BigDecimalFormatter.EMPTY_BALANCE_SIGN

        return BigDecimalFormatter.formatPercent(
            percent = priceChange,
            useAbsoluteValue = true,
        )
    }

    private fun formatPrice(status: CryptoCurrencyStatus.Status, appCurrency: AppCurrency): String {
        val fiatRate = status.fiatRate ?: return BigDecimalFormatter.EMPTY_BALANCE_SIGN

        return BigDecimalFormatter.formatFiatAmount(
            fiatAmount = fiatRate,
            fiatCurrencyCode = appCurrency.code,
            fiatCurrencySymbol = appCurrency.symbol,
        )
    }

    private fun formatFiatAmount(status: CryptoCurrencyStatus.Status, appCurrency: AppCurrency): String {
        val fiatAmount = status.fiatAmount ?: return BigDecimalFormatter.EMPTY_BALANCE_SIGN

        return BigDecimalFormatter.formatFiatAmount(
            fiatAmount = fiatAmount,
            fiatCurrencyCode = appCurrency.code,
            fiatCurrencySymbol = appCurrency.symbol,
        )
    }
}