package com.tangem.feature.wallet.presentation.wallet.state.transformers.converter

import com.tangem.core.ui.components.currency.icon.converter.CryptoCurrencyToIconStateConverter
import com.tangem.core.ui.components.marketprice.PriceChangeType
import com.tangem.core.ui.components.marketprice.utils.PriceChangeConverter
import com.tangem.core.ui.utils.BigDecimalFormatter
import com.tangem.domain.appcurrency.model.AppCurrency
import com.tangem.domain.staking.model.stakekit.YieldBalance
import com.tangem.domain.tokens.model.CryptoCurrencyStatus
import com.tangem.feature.wallet.presentation.common.state.TokenItemState
import com.tangem.feature.wallet.presentation.wallet.viewmodels.intents.WalletClickIntents
import com.tangem.utils.Provider
import com.tangem.utils.StringsSigns.DASH_SIGN
import com.tangem.utils.converter.Converter
import com.tangem.utils.extensions.orZero
import java.math.BigDecimal

internal class TokenItemStateConverter(
    private val appCurrencyProvider: Provider<AppCurrency>,
    private val clickIntents: WalletClickIntents,
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
            onItemClick = { clickIntents.onTokenItemClick(this) },
            onItemLongClick = { clickIntents.onTokenItemLongClick(cryptoCurrencyStatus = this) },
        )
    }

    private fun CryptoCurrencyStatus.getFormattedAmount(): String {
        val yieldBalance = (value.yieldBalance as? YieldBalance.Data)?.getTotalStakingBalance().orZero()
        val amount = value.amount?.plus(yieldBalance) ?: return DASH_SIGN

        return BigDecimalFormatter.formatCryptoAmount(amount, currency.symbol, currency.decimals)
    }

    private fun CryptoCurrencyStatus.getFormattedFiatAmount(): String {
        val yieldBalance = (value.yieldBalance as? YieldBalance.Data)?.getTotalStakingBalance().orZero()
        val fiatYieldBalance = value.fiatRate?.times(yieldBalance).orZero()
        val fiatAmount = value.fiatAmount?.plus(fiatYieldBalance) ?: return DASH_SIGN
        val appCurrency = appCurrencyProvider()

        return BigDecimalFormatter.formatFiatAmount(fiatAmount, appCurrency.code, appCurrency.symbol)
    }

    private fun CryptoCurrencyStatus.mapToUnreachableTokenItemState() = TokenItemState.Unreachable(
        id = currency.id.value,
        iconState = iconStateConverter.convert(value = this),
        titleState = TokenItemState.TitleState.Content(text = currency.name),
        onItemClick = { clickIntents.onTokenItemClick(this) },
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
                ),
                type = priceChange.getPriceChangeType(),
            )
        } else {
            TokenItemState.CryptoPriceState.Unknown
        }
    }

    private fun BigDecimal.getFormattedCryptoPrice(): String {
        val appCurrency = appCurrencyProvider()
        return BigDecimalFormatter.formatFiatAmountUncapped(
            fiatAmount = this,
            fiatCurrencyCode = appCurrency.code,
            fiatCurrencySymbol = appCurrency.symbol,
        )
    }

    private fun BigDecimal.getPriceChangeType(): PriceChangeType {
        return PriceChangeConverter.fromBigDecimal(value = this)
    }
}
