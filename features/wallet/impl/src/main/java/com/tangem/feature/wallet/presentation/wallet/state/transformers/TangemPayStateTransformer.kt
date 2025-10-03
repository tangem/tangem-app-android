package com.tangem.feature.wallet.presentation.wallet.state.transformers

import com.tangem.common.ui.R
import com.tangem.core.ui.extensions.TextReference
import com.tangem.core.ui.format.bigdecimal.fiat
import com.tangem.core.ui.format.bigdecimal.format
import com.tangem.domain.pay.model.CustomerInfo.CardInfo
import com.tangem.domain.pay.model.MainScreenCustomerInfo
import com.tangem.domain.pay.model.OrderStatus.CANCELED
import com.tangem.domain.pay.model.OrderStatus.NOT_ISSUED
import com.tangem.feature.wallet.presentation.wallet.state.model.TangemPayState
import com.tangem.feature.wallet.presentation.wallet.state.model.TangemPayState.Progress
import com.tangem.feature.wallet.presentation.wallet.state.model.WalletScreenState
import java.util.Currency

internal class TangemPayStateTransformer(
    private val value: MainScreenCustomerInfo? = null,
    private val onIssueOrderClick: () -> Unit = {},
    private val onContinueKycClick: () -> Unit = {},
    private val openDetails: (customerWalletAddress: String, cardNumberEnd: String) -> Unit = { _, _ -> },
    private val issueProgressState: Boolean = false,
    private val issueState: Boolean = false,
) : WalletScreenStateTransformer {

    override fun transform(prevState: WalletScreenState): WalletScreenState {
        val tangemPayState = when {
            issueProgressState -> createIssueProgressState()
            issueState -> createIssueState()
            else -> createInitialState()
        }
        return prevState.copy(tangemPayState = tangemPayState)
    }

    private fun createInitialState(): TangemPayState {
        val cardInfo = value?.info?.cardInfo
        return when {
            value == null -> TangemPayState.Empty
            !value.info.isKycApproved() -> createKycInProgressState(onContinueKycClick)
            cardInfo != null -> getCardInfoState(cardInfo)
            value.orderStatus == NOT_ISSUED || value.orderStatus == CANCELED -> createIssueState()
            else -> createIssueProgressState()
        }
    }

    private fun createIssueProgressState(): TangemPayState = Progress(
        title = TextReference.Res(R.string.tangempay_issue_card_notification_title),
        buttonText = TextReference.EMPTY,
        iconRes = R.drawable.ic_tangem_pay_promo_card_36,
        onButtonClick = {},
        showProgress = true,
    )

    private fun createIssueState() = Progress(
        title = TextReference.Res(R.string.tangempay_issue_card_notification_title),
        buttonText = TextReference.Res(R.string.common_continue),
        iconRes = R.drawable.ic_tangem_pay_promo_card_36,
        onButtonClick = onIssueOrderClick,
    )

    private fun createKycInProgressState(onContinueKycClick: () -> Unit): TangemPayState = Progress(
        title = TextReference.Res(R.string.tangempay_kyc_in_progress_notification_title),
        buttonText = TextReference.Res(R.string.tangempay_kyc_in_progress_notification_button),
        iconRes = R.drawable.ic_promo_kyc_36,
        onButtonClick = onContinueKycClick,
    )

    private fun getCardInfoState(cardInfo: CardInfo): TangemPayState = TangemPayState.Card(
        lastFourDigits = TextReference.Str("*${cardInfo.lastFourDigits}"),
        balanceText = TextReference.Str(getBalanceText(cardInfo)),
        onClick = { openDetails(cardInfo.customerWalletAddress, cardInfo.lastFourDigits) },
    )

    private fun getBalanceText(cardInfo: CardInfo): String {
        val currency = Currency.getInstance(cardInfo.currencyCode)
        return cardInfo.balance.format {
            fiat(fiatCurrencyCode = currency.currencyCode, fiatCurrencySymbol = currency.symbol)
        }
    }
}