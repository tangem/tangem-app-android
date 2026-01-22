package com.tangem.feature.wallet.presentation.wallet.state.transformers

import com.tangem.common.ui.R
import com.tangem.core.ui.extensions.TextReference
import com.tangem.core.ui.extensions.stringReference
import com.tangem.core.ui.format.bigdecimal.fiat
import com.tangem.core.ui.format.bigdecimal.format
import com.tangem.domain.models.wallet.UserWalletId
import com.tangem.domain.pay.TangemPayDetailsConfig
import com.tangem.domain.pay.model.CustomerInfo.CardInfo
import com.tangem.domain.pay.model.CustomerInfo.ProductInstance
import com.tangem.domain.pay.model.MainScreenCustomerInfo
import com.tangem.domain.pay.model.OrderStatus
import com.tangem.domain.visa.model.TangemPayCardFrozenState
import com.tangem.feature.wallet.child.wallet.model.intents.TangemPayIntents
import com.tangem.feature.wallet.presentation.wallet.state.model.TangemPayState
import com.tangem.feature.wallet.presentation.wallet.state.model.TangemPayState.Progress
import com.tangem.feature.wallet.presentation.wallet.state.model.WalletState
import com.tangem.domain.pay.model.CustomerInfo.KycStatus.APPROVED
import com.tangem.domain.pay.model.CustomerInfo
import java.util.Currency

/**
 * Hardcode Polygon chain id only for F&F.
 * Later chain id will be fetched from BFF.
 */
private const val POLYGON_CHAIN_ID = 137

internal class TangemPayUpdateInfoStateTransformer(
    userWalletId: UserWalletId,
    private val value: MainScreenCustomerInfo,
    private val cardFrozenState: TangemPayCardFrozenState,
    private val tangemPayClickIntents: TangemPayIntents,
) : WalletStateTransformer(userWalletId = userWalletId) {

    override fun transform(prevState: WalletState): WalletState {
        val tangemPayState = createInitialState()
        return if (prevState is WalletState.MultiCurrency.Content) {
            prevState.copy(tangemPayState = tangemPayState)
        } else {
            prevState
        }
    }

    private fun createInitialState(): TangemPayState {
        val cardInfo = value.info.cardInfo
        val productInstance = value.info.productInstance
        val customerId = value.info.customerId

        // when statement copied to WalletTangemPayAnalyticsEventSender. Be careful when editing.
        return when {
            value.orderStatus == OrderStatus.CANCELED -> createCancelledState()
            value.info.kycStatus != APPROVED && !customerId.isNullOrEmpty() ->
                createKycInProgressState(kycStatus = value.info.kycStatus, customerId = customerId)
            cardInfo != null && productInstance != null -> getCardInfoState(cardInfo, productInstance)
            else -> createIssueProgressState()
        }
    }

    private fun getCardInfoState(cardInfo: CardInfo, productInstance: ProductInstance): TangemPayState =
        TangemPayState.Card(
            lastFourDigits = TextReference.Str("*${cardInfo.lastFourDigits}"),
            balanceText = TextReference.Str(getBalanceText(cardInfo)),
            balanceSymbol = stringReference("USDC"), // TODO hardcode for now
            onClick = {
                tangemPayClickIntents.openDetails(
                    userWalletId,
                    TangemPayDetailsConfig(
                        cardId = productInstance.cardId,
                        isPinSet = cardInfo.isPinSet,
                        cardFrozenState = cardFrozenState,
                        customerWalletAddress = cardInfo.customerWalletAddress,
                        cardNumberEnd = cardInfo.lastFourDigits,
                        chainId = POLYGON_CHAIN_ID,
                    ),
                )
            },
        )

    private fun getBalanceText(cardInfo: CardInfo): String {
        val currency = Currency.getInstance(cardInfo.currencyCode)
        return cardInfo.balance.format {
            fiat(fiatCurrencyCode = currency.currencyCode, fiatCurrencySymbol = currency.symbol)
        }
    }

    private fun createKycInProgressState(kycStatus: CustomerInfo.KycStatus, customerId: String): TangemPayState =
        Progress(
            title = TextReference.Res(R.string.tangempay_payment_account),
            description = when (kycStatus) {
                CustomerInfo.KycStatus.REJECTED -> TextReference.Res(R.string.tangempay_kyc_has_failed)
                else -> TextReference.Res(R.string.tangempay_kyc_in_progress)
            },
            buttonText = TextReference.Res(R.string.tangempay_kyc_in_progress_notification_button),
            iconRes = R.drawable.ic_promo_kyc_36,
            onButtonClick = {
                when (kycStatus) {
                    CustomerInfo.KycStatus.REJECTED -> tangemPayClickIntents.onKycRejectedClicked(
                        userWalletId = userWalletId,
                        customerId = customerId,
                    )
                    else -> tangemPayClickIntents.onKycProgressClicked(userWalletId)
                }
            },
        )

    private fun createIssueProgressState(): TangemPayState = Progress(
        title = TextReference.Res(R.string.tangempay_payment_account),
        description = TextReference.Res(R.string.tangempay_issuing_your_card),
        buttonText = TextReference.EMPTY,
        iconRes = R.drawable.ic_tangem_pay_promo_card_36,
        onButtonClick = tangemPayClickIntents::onIssuingCardClicked,
        showProgress = true,
    )

    private fun createCancelledState(): TangemPayState = TangemPayState.FailedIssue(
        title = TextReference.Res(R.string.tangempay_payment_account),
        description = TextReference.Res(R.string.tangempay_failed_to_issue_card),
        iconRes = R.drawable.ic_alert_24,
        onButtonClick = tangemPayClickIntents::onIssuingFailedClicked,
    )
}