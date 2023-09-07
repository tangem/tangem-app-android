package com.tangem.feature.wallet.presentation.wallet.state.factory

import arrow.core.Either
import com.tangem.common.Provider
import com.tangem.core.ui.components.marketprice.MarketPriceBlockState
import com.tangem.core.ui.components.marketprice.PriceChangeConfig
import com.tangem.core.ui.utils.BigDecimalFormatter
import com.tangem.domain.appcurrency.model.AppCurrency
import com.tangem.domain.common.CardTypesResolver
import com.tangem.domain.tokens.error.CurrencyStatusError
import com.tangem.domain.tokens.model.CryptoCurrencyStatus
import com.tangem.domain.wallets.models.UserWallet
import com.tangem.feature.wallet.presentation.wallet.domain.WalletAdditionalInfoFactory
import com.tangem.feature.wallet.presentation.wallet.state.WalletMultiCurrencyState
import com.tangem.feature.wallet.presentation.wallet.state.WalletSingleCurrencyState
import com.tangem.feature.wallet.presentation.wallet.state.WalletState
import com.tangem.feature.wallet.presentation.wallet.state.components.WalletCardState
import com.tangem.feature.wallet.presentation.wallet.state.components.WalletsListConfig
import com.tangem.feature.wallet.presentation.wallet.utils.CurrencyStatusErrorConverter
import com.tangem.utils.converter.Converter
import kotlinx.collections.immutable.toPersistentList
import java.math.BigDecimal

internal class WalletSingleCurrencyLoadedBalanceConverter(
    private val currentStateProvider: Provider<WalletState>,
    private val cardTypeResolverProvider: Provider<CardTypesResolver>,
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
                val currencyName = state.marketPriceBlockState.currencyName

                state.copy(
                    walletsListConfig = getUpdatedSelectedWallet(status = status.value, state = state),
                    marketPriceBlockState = getMarketPriceState(status = status.value, currencyName = currencyName),
                )
            }
            is WalletMultiCurrencyState.Content,
            is WalletMultiCurrencyState.Locked,
            is WalletSingleCurrencyState.Locked,
            is WalletState.Initial,
            -> state
        }
    }

    private fun getMarketPriceState(status: CryptoCurrencyStatus.Status, currencyName: String): MarketPriceBlockState {
        return when (status) {
            is CryptoCurrencyStatus.NoQuote,
            is CryptoCurrencyStatus.Loaded,
            -> MarketPriceBlockState.Content(
                currencyName = currencyName,
                price = formatPrice(status, appCurrencyProvider()),
                priceChangeConfig = PriceChangeConfig(
                    valueInPercent = formatPriceChange(status),
                    type = getPriceChangeType(status),
                ),
            )
            is CryptoCurrencyStatus.Loading -> MarketPriceBlockState.Loading(currencyName)
            is CryptoCurrencyStatus.Custom,
            is CryptoCurrencyStatus.MissedDerivation,
            is CryptoCurrencyStatus.NoAccount,
            is CryptoCurrencyStatus.Unreachable,
            -> MarketPriceBlockState.Error(currencyName)
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
            -> {
                WalletCardState.Content(
                    id = selectedWallet.id,
                    title = selectedWallet.title,
                    additionalInfo = WalletAdditionalInfoFactory.resolve(
                        cardTypesResolver = cardTypeResolverProvider(),
                        wallet = currentWalletProvider(),
                        currencyAmount = status.amount,
                    ),
                    imageResId = selectedWallet.imageResId,
                    onRenameClick = selectedWallet.onRenameClick,
                    onDeleteClick = selectedWallet.onDeleteClick,
                    balance = formatFiatAmount(status = status, appCurrency = appCurrencyProvider()),
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
            is CryptoCurrencyStatus.NoAccount,
            is CryptoCurrencyStatus.Custom,
            is CryptoCurrencyStatus.Unreachable,
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

    private fun getPriceChangeType(status: CryptoCurrencyStatus.Status): PriceChangeConfig.Type {
        val priceChange = status.priceChange ?: return PriceChangeConfig.Type.DOWN

        return if (priceChange > BigDecimal.ZERO) {
            PriceChangeConfig.Type.UP
        } else {
            PriceChangeConfig.Type.DOWN
        }
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
