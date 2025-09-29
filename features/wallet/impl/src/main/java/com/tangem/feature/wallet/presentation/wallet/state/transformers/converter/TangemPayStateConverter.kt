package com.tangem.feature.wallet.presentation.wallet.state.transformers.converter

import com.tangem.common.ui.R
import com.tangem.core.ui.extensions.TextReference
import com.tangem.core.ui.format.bigdecimal.fiat
import com.tangem.core.ui.format.bigdecimal.format
import com.tangem.domain.pay.model.CustomerInfo.CardInfo
import com.tangem.domain.pay.model.MainScreenCustomerInfo
import com.tangem.domain.pay.model.OrderStatus
import com.tangem.feature.wallet.presentation.wallet.state.model.TangemPayState
import com.tangem.feature.wallet.presentation.wallet.state.model.TangemPayState.Progress
import java.util.Currency
import javax.inject.Inject

internal class TangemPayStateConverter @Inject constructor() {

    fun convert(
        value: MainScreenCustomerInfo,
        onIssueOrderClick: () -> Unit,
        onContinueKycClick: () -> Unit,
    ): TangemPayState {
        val cardInfo = value.info.cardInfo
        return when {
            !value.info.isKycApproved() -> {
                Progress(
                    title = TextReference.Res(R.string.tangempay_kyc_in_progress_notification_title),
                    buttonText = TextReference.Res(R.string.tangempay_kyc_in_progress_notification_button),
                    iconRes = R.drawable.ic_promo_kyc_36,
                    onButtonClick = onContinueKycClick,
                )
            }
            value.orderStatus == OrderStatus.NOT_ISSUED || value.orderStatus == OrderStatus.CANCELED -> {
                getIssueState(onIssueOrderClick)
            }
            cardInfo != null -> {
                TangemPayState.Card(
                    lastFourDigits = TextReference.Str("*${cardInfo.lastFourDigits}"),
                    balanceText = TextReference.Str(getBalanceText(cardInfo)),
                )
            }
            else -> {
                getIssueProgress()
            }
        }
    }

    fun getIssueProgress(): TangemPayState = Progress(
        title = TextReference.Res(R.string.tangempay_issue_card_notification_title),
        buttonText = TextReference.EMPTY,
        iconRes = R.drawable.ic_tangem_pay_promo_card_36,
        onButtonClick = {},
        showProgress = true,
    )

    fun getIssueState(onIssueOrderClick: () -> Unit) = Progress(
        title = TextReference.Res(R.string.tangempay_issue_card_notification_title),
        buttonText = TextReference.Res(R.string.common_continue),
        iconRes = R.drawable.ic_tangem_pay_promo_card_36,
        onButtonClick = onIssueOrderClick,
    )

    private fun getBalanceText(cardInfo: CardInfo): String {
        val currency = Currency.getInstance(cardInfo.currencyCode)
        return cardInfo.balance.format {
            fiat(fiatCurrencyCode = currency.currencyCode, fiatCurrencySymbol = currency.symbol)
        }
    }
}