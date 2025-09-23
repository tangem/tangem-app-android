package com.tangem.feature.wallet.presentation.wallet.state.transformers.converter

import com.tangem.common.routing.AppRoute
import com.tangem.common.ui.R
import com.tangem.core.decompose.navigation.Router
import com.tangem.core.ui.extensions.TextReference
import com.tangem.core.ui.format.bigdecimal.fiat
import com.tangem.core.ui.format.bigdecimal.format
import com.tangem.domain.pay.model.CustomerInfo.CardInfo
import com.tangem.domain.pay.model.CustomerInfo
import com.tangem.feature.wallet.presentation.wallet.state.model.TangemPayState
import com.tangem.utils.converter.Converter
import java.util.Currency
import javax.inject.Inject

internal class TangemPayStateConverter @Inject constructor(
    private val router: Router,
) : Converter<CustomerInfo, TangemPayState> {

    override fun convert(value: CustomerInfo): TangemPayState {
        val route = AppRoute.TangemPayOnboarding(AppRoute.TangemPayOnboarding.Mode.ContinueOnboarding)
        val cardInfo = value.cardInfo
        return when {
            !value.isKycApproved() -> TangemPayState.Progress(
                title = TextReference.Res(R.string.tangempay_kyc_in_progress_notification_title),
                buttonText = TextReference.Res(R.string.tangempay_kyc_in_progress_notification_button),
                iconRes = R.drawable.ic_promo_kyc_36,
                onButtonClick = { router.push(route) },
            )
            !value.isProductInstanceActive() -> TangemPayState.Progress(
                title = TextReference.Res(R.string.tangempay_issue_card_notification_title),
                buttonText = TextReference.Res(R.string.common_continue),
                iconRes = R.drawable.ic_tangem_pay_promo_card_36,
                onButtonClick = { router.push(route) },
            )
            cardInfo != null -> {
                TangemPayState.Card(
                    lastFourDigits = TextReference.Str("*${cardInfo.lastFourDigits}"),
                    balanceText = TextReference.Str(getBalanceText(cardInfo)),
                )
            }
            else -> TangemPayState.Empty
        }
    }

    private fun getBalanceText(cardInfo: CardInfo): String {
        val currency = Currency.getInstance(cardInfo.currencyCode)
        return cardInfo.balance.format {
            fiat(fiatCurrencyCode = currency.currencyCode, fiatCurrencySymbol = currency.symbol)
        }
    }
}