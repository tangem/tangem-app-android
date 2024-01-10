package com.tangem.feature.wallet.presentation.wallet.state2.transformers.converter

import com.tangem.core.ui.components.currency.tokenicon.converter.CryptoCurrencyToIconStateConverter
import com.tangem.core.ui.components.marketprice.PriceChangeType
import com.tangem.core.ui.utils.BigDecimalFormatter
import com.tangem.domain.appcurrency.model.AppCurrency
import com.tangem.domain.tokens.model.CryptoCurrencyStatus
import com.tangem.feature.wallet.presentation.common.state.TokenItemState
import com.tangem.feature.wallet.presentation.wallet.viewmodels.intents.WalletClickIntentsV2
import com.tangem.utils.Provider
import com.tangem.utils.converter.Converter
import java.math.BigDecimal

internal class TokenItemStateConverter(
    private val appCurrencyProvider: Provider<AppCurrency>,
    private val clickIntents: WalletClickIntentsV2,
) : Converter<CryptoCurrencyStatus, TokenItemState> {

    private val iconStateConverter by lazy(::CryptoCurrencyToIconStateConverter)

    override fun convert(value: CryptoCurrencyStatus): TokenItemState {
        return when (value.value) {
            is CryptoCurrencyStatus.Loading -> value.mapToLoadingState()
            is CryptoCurrencyStatus.Loaded,
            is CryptoCurrencyStatus.Custom,
            is CryptoCurrencyStatus.NoQuote,
            is CryptoCurrencyStatus.NoAccount,
            -> value.mapToTokenItemState()
            is CryptoCurrencyStatus.MissedDerivation -> value.mapToNoAddressTokenItemState()
            is CryptoCurrencyStatus.Unreachable,
            is CryptoCurrencyStatus.UnreachableWithoutAddresses,
            is CryptoCurrencyStatus.NoAmount,
            -> value.mapToUnreachableTokenItemState()
        }
    }

    private fun CryptoCurrencyStatus.mapToLoadingState(): TokenItemState.Loading {
        return TokenItemState.Loading(
            id = currency.id.value,
            iconState = iconStateConverter.convert(value = this),
            titleState = TokenItemState.TitleState.Content(text = currency.name),
        )
    }

    private fun CryptoCurrencyStatus.mapToTokenItemState(): TokenItemState.Content {
        return TokenItemState.Content(
            id = currency.id.value,
            iconState = iconStateConverter.convert(value = this),
            titleState = TokenItemState.TitleState.Content(
                text = currency.name,
                hasPending = value.hasCurrentNetworkTransactions,
            ),
            fiatAmountState = TokenItemState.FiatAmountState.Content(
                text = getFormattedFiatAmount(),
            ),
            cryptoAmountState = TokenItemState.CryptoAmountState.Content(text = getFormattedAmount()),
            cryptoPriceState = getCryptoPriceState(),
            onItemClick = { clickIntents.onTokenItemClick(currency) },
            onItemLongClick = { clickIntents.onTokenItemLongClick(cryptoCurrencyStatus = this) },
        )
    }

    private fun CryptoCurrencyStatus.getFormattedAmount(): String {
        val amount = value.amount ?: return TokenItemState.UNKNOWN_AMOUNT_SIGN

        return BigDecimalFormatter.formatCryptoAmount(amount, currency.symbol, currency.decimals)
    }

    private fun CryptoCurrencyStatus.getFormattedFiatAmount(): String {
        val fiatAmount = value.fiatAmount ?: return TokenItemState.UNKNOWN_AMOUNT_SIGN
        val appCurrency = appCurrencyProvider()

        return BigDecimalFormatter.formatFiatAmount(fiatAmount, appCurrency.code, appCurrency.symbol)
    }

    private fun CryptoCurrencyStatus.mapToUnreachableTokenItemState() = TokenItemState.Unreachable(
        id = currency.id.value,
        iconState = iconStateConverter.convert(value = this),
        titleState = TokenItemState.TitleState.Content(text = currency.name),
        onItemClick = { clickIntents.onTokenItemClick(currency) },
        onItemLongClick = { clickIntents.onTokenItemLongClick(cryptoCurrencyStatus = this) },
    )

    private fun CryptoCurrencyStatus.mapToNoAddressTokenItemState() = TokenItemState.NoAddress(
        id = currency.id.value,
        iconState = iconStateConverter.convert(this),
        titleState = TokenItemState.TitleState.Content(text = currency.name),
        onItemLongClick = { clickIntents.onTokenItemLongClick(cryptoCurrencyStatus = this) },
    )

    private fun CryptoCurrencyStatus.getCryptoPriceState(): TokenItemState.CryptoPriceState {
        val fiatRate = value.fiatRate
        val priceChange = value.priceChange

        return if (fiatRate != null && priceChange != null) {
            TokenItemState.CryptoPriceState.Content(
                price = fiatRate.getFormattedCryptoPrice(),
                priceChangePercent = BigDecimalFormatter.formatPercent(
                    percent = priceChange,
                    useAbsoluteValue = true,
                    maxFractionDigits = 1,
                    minFractionDigits = 1,
                ),
                type = priceChange.getPriceChangeType(),
            )
        } else {
            TokenItemState.CryptoPriceState.Unknown
        }
    }

    private fun BigDecimal.getFormattedCryptoPrice(): String {
        val appCurrency = appCurrencyProvider()
        return BigDecimalFormatter.formatFiatAmount(
            fiatAmount = this,
            fiatCurrencyCode = appCurrency.code,
            fiatCurrencySymbol = appCurrency.symbol,
        )
    }

    private fun BigDecimal.getPriceChangeType(): PriceChangeType {
        return if (this > BigDecimal.ZERO) PriceChangeType.UP else PriceChangeType.DOWN
    }
}