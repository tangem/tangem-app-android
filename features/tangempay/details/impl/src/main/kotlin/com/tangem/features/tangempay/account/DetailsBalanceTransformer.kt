package com.tangem.features.tangempay.account

import com.tangem.core.ui.extensions.TextReference
import com.tangem.core.ui.format.bigdecimal.fiat
import com.tangem.core.ui.format.bigdecimal.formatStyled
import com.tangem.core.ui.res.TangemTheme
import com.tangem.domain.models.account.PaymentAccountStatusValue
import com.tangem.utils.transformer.Transformer
import java.util.Currency

internal class DetailsBalanceTransformer(
    private val fiatBalance: PaymentAccountStatusValue.FiatBalance,
    private val isInactive: Boolean = false,
    private val isMuted: Boolean = false,
) : Transformer<TangemPayDetailsUM> {

    override fun transform(prevState: TangemPayDetailsUM): TangemPayDetailsUM {
        val balance = TangemPayDetailsBalanceBlockState.Content(
            isBalanceFlickering = false,
            fiatBalance = getFiatBalanceText(fiatBalance),
            actionButtons = prevState.balanceBlockState.actionButtons,
            cardsBlockState = prevState.balanceBlockState.cardsBlockState,
            isMuted = isMuted,
            isNegative = fiatBalance.availableBalance.signum() < 0,
            isInactive = isInactive,
        )
        return prevState.copy(balanceBlockState = balance)
    }

    companion object {
        fun getFiatBalanceText(fiatBalance: PaymentAccountStatusValue.FiatBalance): TextReference {
            val currency = Currency.getInstance(fiatBalance.currency)
            return fiatBalance.availableBalance.formatStyled {
                fiat(
                    fiatCurrencyCode = currency.currencyCode,
                    fiatCurrencySymbol = currency.symbol,
                    spanStyleReference = { TangemTheme.typography3.heading.medium.toSpanStyle() },
                )
            }
        }
    }
}