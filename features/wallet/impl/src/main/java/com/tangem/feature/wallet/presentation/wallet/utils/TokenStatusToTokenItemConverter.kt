package com.tangem.feature.wallet.presentation.wallet.utils

import androidx.annotation.DrawableRes
import com.tangem.core.ui.components.marketprice.PriceChangeConfig
import com.tangem.core.ui.utils.BigDecimalFormatter
import com.tangem.domain.tokens.model.TokenStatus
import com.tangem.feature.wallet.impl.R
import com.tangem.feature.wallet.presentation.common.state.TokenItemState
import com.tangem.utils.converter.Converter
import java.math.BigDecimal

internal class TokenStatusToTokenItemConverter(
    private val isWalletContentHidden: Boolean,
    private val fiatCurrencyCode: String,
    private val fiatCurrencySymbol: String,
) : Converter<TokenStatus, TokenItemState> {

    private val TokenStatus.networkIconResId: Int?
        @DrawableRes get() {
// [REDACTED_TODO_COMMENT]
            return if (isCoin) null else R.drawable.img_eth_22
        }

    private val TokenStatus.tokenIconResId: Int
        @DrawableRes get() {
// [REDACTED_TODO_COMMENT]
            return R.drawable.img_eth_22
        }

    override fun convert(value: TokenStatus): TokenItemState {
        return when (value.value) {
            is TokenStatus.Loading -> TokenItemState.Loading
            is TokenStatus.Loaded,
            is TokenStatus.Custom,
            -> value.mapToTokenItemState()
// [REDACTED_TODO_COMMENT]
            is TokenStatus.MissedDerivation,
            is TokenStatus.NoAccount,
            is TokenStatus.Unreachable,
            -> value.mapToUnreachableTokenItemState()
        }
    }

    private fun TokenStatus.mapToTokenItemState(): TokenItemState.Content {
        return TokenItemState.Content(
            id = this.id.value,
            name = this.name,
            tokenIconUrl = this.iconUrl,
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

    private fun TokenStatus.getFormattedAmount(): String {
        val amount = value.amount ?: return UNKNOWN_AMOUNT_SIGN

        return BigDecimalFormatter.formatCryptoAmount(amount, symbol, decimals)
    }

    private fun TokenStatus.getFormattedFiatAmount(): String {
        val fiatAmount = value.fiatAmount ?: return UNKNOWN_AMOUNT_SIGN

        return BigDecimalFormatter.formatFiatAmount(fiatAmount, fiatCurrencyCode, fiatCurrencySymbol)
    }

    private fun TokenStatus.mapToUnreachableTokenItemState() = TokenItemState.Unreachable(
        id = this.id.value,
        name = this.name,
        tokenIconUrl = this.iconUrl,
        tokenIconResId = this.tokenIconResId,
        networkIconResId = this.networkIconResId,
    )

    private fun TokenStatus.getPriceChangeConfig(): PriceChangeConfig {
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
