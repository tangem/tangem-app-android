package com.tangem.feature.wallet.presentation.wallet.state.factory

import arrow.core.Either
import com.tangem.common.Provider
import com.tangem.core.ui.components.marketprice.MarketPriceBlockState
import com.tangem.core.ui.components.marketprice.PriceChangeConfig
import com.tangem.core.ui.utils.BigDecimalFormatter
import com.tangem.domain.common.CardTypesResolver
import com.tangem.domain.tokens.error.CurrencyError
import com.tangem.domain.tokens.model.CryptoCurrencyStatus
import com.tangem.feature.wallet.presentation.wallet.domain.WalletAdditionalInfoFactory
import com.tangem.feature.wallet.presentation.wallet.state.WalletSingleCurrencyState
import com.tangem.feature.wallet.presentation.wallet.state.WalletState
import com.tangem.feature.wallet.presentation.wallet.state.components.WalletCardState
import com.tangem.feature.wallet.presentation.wallet.state.components.WalletsListConfig
import com.tangem.utils.converter.Converter
import kotlinx.collections.immutable.toPersistentList
import java.math.BigDecimal

internal class WalletSingleCurrencyLoadedBalanceConverter(
    private val currentStateProvider: Provider<WalletState>,
    private val cardTypeResolverProvider: Provider<CardTypesResolver>,
    private val fiatCurrencyCode: String,
    private val fiatCurrencySymbol: String,
) : Converter<Either<CurrencyError, CryptoCurrencyStatus>, WalletSingleCurrencyState.Content> {

    override fun convert(value: Either<CurrencyError, CryptoCurrencyStatus>): WalletSingleCurrencyState.Content {
        return value.fold(ifLeft = { convertError() }, ifRight = ::convert)
    }

    private fun convertError(): WalletSingleCurrencyState.Content {
        return requireNotNull(currentStateProvider() as? WalletSingleCurrencyState.Content)
    }

    private fun convert(status: CryptoCurrencyStatus): WalletSingleCurrencyState.Content {
        val state = requireNotNull(currentStateProvider() as? WalletSingleCurrencyState.Content)
        val currencyName = state.marketPriceBlockState.currencyName
        return state.copy(
            walletsListConfig = getUpdatedSelectedWallet(status = status.value, state = state),
            marketPriceBlockState = getMarketPriceState(status = status.value, currencyName = currencyName),
        )
    }

    private fun getMarketPriceState(status: CryptoCurrencyStatus.Status, currencyName: String): MarketPriceBlockState {
        return when (status) {
            is CryptoCurrencyStatus.NoQuote,
            is CryptoCurrencyStatus.Loaded,
            -> MarketPriceBlockState.Content(
                currencyName = currencyName,
                price = formatPrice(status),
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
                        isLocked = false,
                        currencyAmount = status.amount,
                    ),
                    imageResId = selectedWallet.imageResId,
                    onClick = selectedWallet.onClick,
                    balance = formatFiatAmount(status),
                )
            }
            is CryptoCurrencyStatus.Loading -> {
                WalletCardState.Loading(
                    id = selectedWallet.id,
                    title = selectedWallet.title,
                    additionalInfo = selectedWallet.additionalInfo,
                    imageResId = selectedWallet.imageResId,
                    onClick = selectedWallet.onClick,
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
                    additionalInfo = selectedWallet.additionalInfo,
                    imageResId = selectedWallet.imageResId,
                    onClick = selectedWallet.onClick,
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

    private fun formatPrice(status: CryptoCurrencyStatus.Status): String {
        val fiatRate = status.fiatRate ?: return BigDecimalFormatter.EMPTY_BALANCE_SIGN

        return BigDecimalFormatter.formatFiatAmount(
            fiatAmount = fiatRate,
            fiatCurrencyCode = fiatCurrencyCode,
            fiatCurrencySymbol = fiatCurrencySymbol,
        )
    }

    private fun formatFiatAmount(status: CryptoCurrencyStatus.Status): String {
        val fiatAmount = status.fiatAmount ?: return BigDecimalFormatter.EMPTY_BALANCE_SIGN

        return BigDecimalFormatter.formatFiatAmount(
            fiatAmount = fiatAmount,
            fiatCurrencyCode = fiatCurrencyCode,
            fiatCurrencySymbol = fiatCurrencySymbol,
        )
    }
}