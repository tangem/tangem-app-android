package com.tangem.feature.wallet.presentation.wallet.utils

import androidx.annotation.DrawableRes
import com.tangem.common.Provider
import com.tangem.core.ui.components.marketprice.PriceChangeConfig
import com.tangem.core.ui.utils.BigDecimalFormatter
import com.tangem.domain.appcurrency.model.AppCurrency
import com.tangem.domain.tokens.model.CryptoCurrencyStatus
import com.tangem.domain.tokens.models.CryptoCurrency
import com.tangem.feature.wallet.impl.R
import com.tangem.feature.wallet.presentation.common.state.TokenItemState
import com.tangem.utils.converter.Converter
import java.math.BigDecimal

internal class CryptoCurrencyStatusToTokenItemConverter(
    private val appCurrencyProvider: Provider<AppCurrency>,
    private val isWalletContentHidden: Boolean,
) : Converter<CryptoCurrencyStatus, TokenItemState> {

    private val CryptoCurrencyStatus.networkIconResId: Int?
        @DrawableRes get() {
// [REDACTED_TODO_COMMENT]
            return if (currency is CryptoCurrency.Coin) null else R.drawable.img_eth_22
        }

    private val CryptoCurrencyStatus.tokenIconResId: Int
        @DrawableRes get() {
// [REDACTED_TODO_COMMENT]
            return R.drawable.img_eth_22
        }

    override fun convert(value: CryptoCurrencyStatus): TokenItemState {
        return when (value.value) {
            is CryptoCurrencyStatus.Loading -> TokenItemState.Loading(id = value.currency.id.value)
            is CryptoCurrencyStatus.Loaded,
            is CryptoCurrencyStatus.Custom,
            is CryptoCurrencyStatus.NoQuote,
            -> value.mapToTokenItemState()
// [REDACTED_TODO_COMMENT]
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
            tokenIconUrl = currency.iconUrl,
            tokenIconResId = this.tokenIconResId,
            networkIconResId = this.networkIconResId,
            amount = getFormattedAmount(),
            hasPending = value.hasTransactionsInProgress,
            tokenOptions = if (isWalletContentHidden) {
                TokenItemState.TokenOptionsState.Hidden(getPriceChangeConfig())
            } else {
                TokenItemState.TokenOptionsState.Visible(
                    fiatAmount = getFormattedFiatAmount(),
                    priceChange = getPriceChangeConfig(),
                )
            },
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
        tokenIconUrl = currency.iconUrl,
        tokenIconResId = this.tokenIconResId,
        networkIconResId = this.networkIconResId,
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
