package com.tangem.feature.wallet.presentation.wallet.utils

import com.tangem.common.Provider
import com.tangem.core.ui.components.marketprice.PriceChangeConfig
import com.tangem.core.ui.utils.BigDecimalFormatter
import com.tangem.domain.appcurrency.model.AppCurrency
import com.tangem.domain.tokens.model.CryptoCurrencyStatus
import com.tangem.feature.wallet.presentation.common.state.TokenItemState
import com.tangem.feature.wallet.presentation.common.utils.CryptoCurrencyToIconStateConverter
import com.tangem.feature.wallet.presentation.wallet.viewmodels.WalletClickIntents
import com.tangem.utils.converter.Converter
import java.math.BigDecimal

internal class CryptoCurrencyStatusToTokenItemConverter(
    private val appCurrencyProvider: Provider<AppCurrency>,
    private val isWalletContentHidden: Boolean,
    private val clickIntents: WalletClickIntents,
) : Converter<CryptoCurrencyStatus, TokenItemState> {

    private val iconStateConverter = CryptoCurrencyToIconStateConverter()

    override fun convert(value: CryptoCurrencyStatus): TokenItemState {
        return when (value.value) {
            is CryptoCurrencyStatus.Loading -> TokenItemState.Loading(id = value.currency.id.value)
            is CryptoCurrencyStatus.Loaded,
            is CryptoCurrencyStatus.Custom,
            is CryptoCurrencyStatus.NoQuote,
            -> value.mapToTokenItemState()
            // TODO: Add other token item states, currently not designed
            is CryptoCurrencyStatus.MissedDerivation,
            is CryptoCurrencyStatus.NoAccount,
            is CryptoCurrencyStatus.Unreachable,
            -> value.mapToUnreachableTokenItemState()
        }
    }

    private fun CryptoCurrencyStatus.mapToTokenItemState(): TokenItemState.Content {
        return TokenItemState.Content(
            id = currency.id.value,
            name = currency.name,
            icon = iconStateConverter.convert(currency),
            amount = getFormattedAmount(),
            hasPending = value.hasCurrentNetworkTransactions,
            tokenOptions = if (isWalletContentHidden) {
                TokenItemState.TokenOptionsState.Hidden(getPriceChangeConfig())
            } else {
                TokenItemState.TokenOptionsState.Visible(
                    fiatAmount = getFormattedFiatAmount(),
                    config = getPriceChangeConfig(),
                )
            },
            onItemClick = { clickIntents.onTokenItemClick(currency) },
            onItemLongClick = { clickIntents.onTokenItemLongClick(currency) },
        )
    }

    private fun CryptoCurrencyStatus.getFormattedAmount(): String {
        val amount = value.amount ?: return UNKNOWN_AMOUNT_SIGN

        return BigDecimalFormatter.formatCryptoAmount(amount, currency.symbol, currency.decimals)
    }

    private fun CryptoCurrencyStatus.getFormattedFiatAmount(): String {
        val fiatAmount = value.fiatAmount ?: return UNKNOWN_AMOUNT_SIGN
        val appCurrency = appCurrencyProvider()

        return BigDecimalFormatter.formatFiatAmount(fiatAmount, appCurrency.code, appCurrency.symbol)
    }

    private fun CryptoCurrencyStatus.mapToUnreachableTokenItemState() = TokenItemState.Unreachable(
        id = currency.id.value,
        name = currency.name,
        icon = iconStateConverter.convert(currency),
    )

    private fun CryptoCurrencyStatus.getPriceChangeConfig(): PriceChangeConfig {
        val priceChange = value.priceChange
            ?: return PriceChangeConfig(UNKNOWN_AMOUNT_SIGN, PriceChangeConfig.Type.DOWN)

        return PriceChangeConfig(
            valueInPercent = BigDecimalFormatter.formatPercent(priceChange, useAbsoluteValue = true),
            type = priceChange.getPriceChangeType(),
        )
    }

    private fun BigDecimal?.getPriceChangeType(): PriceChangeConfig.Type {
        return when {
            this == null -> PriceChangeConfig.Type.DOWN
            this < BigDecimal.ZERO -> PriceChangeConfig.Type.DOWN
            else -> PriceChangeConfig.Type.UP
        }
    }

    private companion object {
        const val UNKNOWN_AMOUNT_SIGN = "—"
    }
}