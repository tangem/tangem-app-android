package com.tangem.features.tangempay.campaign

import com.tangem.common.routing.AppRoute
import com.tangem.core.decompose.di.ModelScoped
import com.tangem.core.decompose.model.Model
import com.tangem.core.decompose.model.ParamsContainer
import com.tangem.core.decompose.navigation.Router
import com.tangem.core.decompose.ui.UiMessageSender
import com.tangem.core.ui.format.bigdecimal.fiat
import com.tangem.core.ui.format.bigdecimal.format
import com.tangem.core.ui.format.bigdecimal.optionalDecimals
import com.tangem.core.ui.message.BottomSheetMessage
import com.tangem.domain.models.account.AccountStatus
import com.tangem.domain.models.account.isPlanTransitioningState
import com.tangem.domain.models.pay.TangemPayDetailsInitialRoute
import com.tangem.domain.pay.flow.PaymentAccountStatusFetcher
import com.tangem.domain.pay.flow.PaymentAccountStatusSupplier
import com.tangem.domain.pay.model.IssueAdditionalCardError
import com.tangem.domain.pay.usecase.IssueAdditionalCardUseCase
import com.tangem.features.tangempay.common.TangemPayCampaignMessages
import com.tangem.features.tangempay.common.TangemPayMessagesFactory
import com.tangem.features.tangempay.common.tariffPlanState
import com.tangem.features.tangempay.components.TangemPayCampaignIssueCardComponent
import com.tangem.utils.coroutines.CoroutineDispatcherProvider
import com.tangem.utils.logging.TangemLogger
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@ModelScoped
@Suppress("LongParameterList")
internal class TangemPayCampaignIssueCardModel @Inject constructor(
    paramsContainer: ParamsContainer,
    override val dispatchers: CoroutineDispatcherProvider,
    private val router: Router,
    private val uiMessageSender: UiMessageSender,
    private val issueAdditionalCardUseCase: IssueAdditionalCardUseCase,
    private val paymentAccountStatusFetcher: PaymentAccountStatusFetcher,
    private val paymentAccountStatusSupplier: PaymentAccountStatusSupplier,
) : Model() {

    private val params = paramsContainer.require<TangemPayCampaignIssueCardComponent.Params>()

    val uiState: StateFlow<TangemPayCampaignIssueCardUM>
        field = MutableStateFlow(
            TangemPayCampaignIssueCardUM(
                cardArtUrl = params.cardArtUrl,
                issueFee = params.feeAmount.format {
                    fiat(
                        fiatCurrencyCode = params.feeCurrency.currencyCode,
                        fiatCurrencySymbol = params.feeCurrency.symbol,
                    ).optionalDecimals()
                },
                isIssuing = false,
                onIssueClick = ::onIssueClick,
                onCloseClick = ::onClose,
            ),
        )

    fun onClose() {
        modelScope.launch { finish(initialRoute = TangemPayDetailsInitialRoute.ACCOUNT_DETAILS) }
    }

    private fun onIssueClick() {
        if (uiState.value.isIssuing) return
        uiState.update { it.copy(isIssuing = true) }
        modelScope.launch {
            issueAdditionalCardUseCase(userWalletId = params.userWalletId, deeplinkValue = params.campaignKey).fold(
                ifLeft = { error -> handleIssueError(error) },
                ifRight = {
                    TangemLogger.i("[TangemPay][Campaign] Campaign card issue order created or resumed")
                    finish(initialRoute = TangemPayDetailsInitialRoute.ACCOUNT_DETAILS)
                },
            )
        }
    }

    private suspend fun handleIssueError(error: IssueAdditionalCardError) {
        TangemLogger.e("[TangemPay][Campaign] Failed to issue the campaign card: $error")
        uiState.update { it.copy(isIssuing = false) }
        val message = when (error) {
            IssueAdditionalCardError.OfferNotAvailable -> createCardsLimitMessage()
            IssueAdditionalCardError.NoCampaignOffer -> TangemPayCampaignMessages.unavailableDialog(
                onDismiss = ::onClose,
            )
            is IssueAdditionalCardError.Other -> TangemPayCampaignMessages.retryableErrorSnackbar()
        }
        uiMessageSender.send(message)
    }

    private suspend fun createCardsLimitMessage(): BottomSheetMessage {
        val tariffState = currentStatus()?.tariffPlanState
        return if (tariffState != null && tariffState.tariff.plan.isBasicTier) {
            TangemPayMessagesFactory.createMaximumCardsForPlanIssuedMessage(
                onUpgradeClick = { onUpgradePlanClick() }.takeIf { !tariffState.isPlanTransitioningState },
            )
        } else {
            TangemPayMessagesFactory.createMaximumCardsIssuedMessage()
        }
    }

    private fun onUpgradePlanClick() {
        modelScope.launch { finish(initialRoute = TangemPayDetailsInitialRoute.CURRENT_PLAN) }
    }

    private suspend fun finish(initialRoute: TangemPayDetailsInitialRoute) {
        paymentAccountStatusFetcher.invoke(params.userWalletId)
        val status = currentStatus()
        if (status == null) {
            router.replaceAll(AppRoute.Wallet)
        } else {
            router.replaceAll(AppRoute.Wallet, AppRoute.TangemPayDetails(status = status, initialRoute = initialRoute))
        }
    }

    private suspend fun currentStatus(): AccountStatus.Payment? {
        return paymentAccountStatusSupplier.invoke(params.userWalletId).firstOrNull()
    }
}