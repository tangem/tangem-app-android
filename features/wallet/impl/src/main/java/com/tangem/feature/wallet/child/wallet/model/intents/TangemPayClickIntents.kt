package com.tangem.feature.wallet.child.wallet.model.intents

import com.arkivanov.decompose.router.slot.activate
import com.tangem.common.routing.AppRoute
import com.tangem.core.analytics.api.AnalyticsEventHandler
import com.tangem.core.analytics.models.AnalyticsParam
import com.tangem.core.analytics.models.Basic
import com.tangem.core.decompose.di.ModelScoped
import com.tangem.core.decompose.ui.UiMessageSender
import com.tangem.core.res.R
import com.tangem.core.ui.components.bottomsheets.message.*
import com.tangem.core.ui.extensions.resourceReference
import com.tangem.core.ui.message.DialogMessage
import com.tangem.core.ui.message.EventMessageAction
import com.tangem.core.ui.message.ToastMessage
import com.tangem.core.ui.message.bottomSheetMessage
import com.tangem.domain.feedback.GetWalletMetaInfoUseCase
import com.tangem.domain.feedback.SendFeedbackEmailUseCase
import com.tangem.domain.feedback.models.FeedbackEmailType
import com.tangem.domain.feedback.models.WalletMetaInfo
import com.tangem.domain.models.wallet.UserWallet
import com.tangem.domain.models.wallet.UserWalletId
import com.tangem.domain.pay.TangemPayDetailsConfig
import com.tangem.domain.pay.TangemPayEligibilityManager
import com.tangem.domain.pay.model.TangemPayEntryPoint
import com.tangem.domain.pay.flow.PaymentAccountStatusFetcher
import com.tangem.domain.pay.repository.OnboardingRepository
import com.tangem.domain.pay.usecase.ProduceTangemPayInitialDataUseCase
import com.tangem.domain.pay.usecase.TangemPayMainScreenCustomerInfoUseCase
import com.tangem.domain.tangempay.TangemPayAnalyticsEvents
import com.tangem.feature.wallet.presentation.wallet.state.WalletStateController
import com.tangem.feature.wallet.presentation.wallet.state.model.WalletDialogConfig
import com.tangem.feature.wallet.presentation.wallet.state.transformers.TangemPayHideOnboardingStateTransformer
import com.tangem.feature.wallet.presentation.wallet.state.transformers.TangemPayRefreshShowProgressTransformer
import kotlinx.coroutines.launch
import javax.inject.Inject

internal interface TangemPayIntents {

    suspend fun onPullToRefresh()

    fun onRefreshPayToken(userWallet: UserWallet)

    fun openDetails(userWalletId: UserWalletId, config: TangemPayDetailsConfig)

    fun onKycProgressClicked(userWalletId: UserWalletId)

    fun onKycRejectedClicked(userWalletId: UserWalletId, customerId: String)

    fun onKycRejectedOpenProfileClicked(userWalletId: UserWalletId)

    fun onKycRejectedGoToSupportClicked(customerId: String)

    fun onKycRejectedHideKycClicked(userWalletId: UserWalletId)

    fun onIssuingCardClicked()

    fun onIssuingFailedClicked(customerId: String)

    fun onPaySupportClick(customerId: String)

    fun onOnboardingBannerClick(userWalletId: UserWalletId)

    fun onOnboardingBannerCloseClick(userWalletId: UserWalletId)
}

@Suppress("LongParameterList")
@ModelScoped
internal class TangemPayClickIntentsImplementor @Inject constructor(
    private val stateHolder: WalletStateController,
    private val onboardingRepository: OnboardingRepository,
    private val produceInitialDataTangemPay: ProduceTangemPayInitialDataUseCase,
    private val getWalletMetainfoUseCase: GetWalletMetaInfoUseCase,
    private val sendFeedbackEmailUseCase: SendFeedbackEmailUseCase,
    private val tangemPayMainScreenCustomerInfoUseCase: TangemPayMainScreenCustomerInfoUseCase,
    private val tangemPayOnboardingRepository: OnboardingRepository,
    private val tangemPayEligibilityManager: TangemPayEligibilityManager,
    private val uiMessageSender: UiMessageSender,
    private val analyticsEventHandler: AnalyticsEventHandler,
    private val paymentAccountStatusFetcher: PaymentAccountStatusFetcher,
) : BaseWalletClickIntents(), TangemPayIntents {

    override suspend fun onPullToRefresh() {
        val userWalletId = stateHolder.getSelectedWalletId()
        if (!onboardingRepository.isTangemPayInitialDataProduced(userWalletId)) {
            return
        }
        tangemPayMainScreenCustomerInfoUseCase.fetch(userWalletId)
        paymentAccountStatusFetcher.invoke(PaymentAccountStatusFetcher.Params(userWalletId))
    }

    override fun onRefreshPayToken(userWallet: UserWallet) {
        stateHolder.update(
            TangemPayRefreshShowProgressTransformer(
                userWalletId = userWallet.walletId,
                shouldShowProgress = true,
            ),
        )

        modelScope.launch {
            produceInitialDataTangemPay.invoke(userWallet.walletId)
                .onRight {
                    tangemPayMainScreenCustomerInfoUseCase.fetch(userWallet.walletId)
                    paymentAccountStatusFetcher.invoke(PaymentAccountStatusFetcher.Params(userWallet.walletId))
                }
                .onLeft {
                    stateHolder.update(
                        TangemPayRefreshShowProgressTransformer(
                            userWalletId = userWallet.walletId,
                            shouldShowProgress = false,
                        ),
                    )
                }
        }
    }

    override fun openDetails(userWalletId: UserWalletId, config: TangemPayDetailsConfig) {
        router.openTangemPayDetails(
            userWalletId = userWalletId,
            config = config,
        )
    }

    override fun onKycProgressClicked(userWalletId: UserWalletId) {
        val cancelKycConfirmDialogMessage = DialogMessage(
            title = resourceReference(R.string.tangempay_kyc_confirm_cancellation_alert_title),
            message = resourceReference(R.string.tangempay_kyc_confirm_cancellation_description),
            firstAction = EventMessageAction(
                title = resourceReference(R.string.common_not_now),
                onClick = { },
            ),
            secondAction = EventMessageAction(
                title = resourceReference(R.string.common_confirm),
                onClick = { disableTangemPay(userWalletId) },
            ),
        )
        val kycInfoBottomSheet = bottomSheetMessage {
            infoBlock {
                iconImage(res = com.tangem.core.ui.R.drawable.img_visa_notification)
                title = resourceReference(R.string.tangempay_kyc_in_progress)
                body = resourceReference(R.string.tangempay_kyc_in_progress_popup_description)
            }
            primaryButton {
                text = resourceReference(R.string.tangempay_kyc_in_progress_notification_button)
                onClick = {
                    router.openTangemPayOnboarding(AppRoute.TangemPayOnboarding.Mode.ContinueOnboarding(userWalletId))
                    closeBs()
                }
            }
            secondaryButton {
                text = resourceReference(R.string.tangempay_cancel_kyc)
                onClick = {
                    uiMessageSender.send(cancelKycConfirmDialogMessage)
                    closeBs()
                }
            }
        }

        uiMessageSender.send(kycInfoBottomSheet)
    }

    override fun onKycRejectedOpenProfileClicked(userWalletId: UserWalletId) {
        router.openTangemPayOnboarding(
            mode = AppRoute.TangemPayOnboarding.Mode.ContinueOnboarding(userWalletId),
        )
    }

    override fun onKycRejectedGoToSupportClicked(customerId: String) {
        goToSupportForRejectKyc(customerId)
    }

    override fun onKycRejectedHideKycClicked(userWalletId: UserWalletId) {
        disableTangemPay(userWalletId)
    }

    override fun onKycRejectedClicked(userWalletId: UserWalletId, customerId: String) {
        router.dialogNavigation.activate(
            configuration = WalletDialogConfig.KycRejected(
                walletId = userWalletId,
                customerId = customerId,
            ),
        )
    }

    private fun goToSupportForRejectKyc(customerId: String) {
        modelScope.launch {
            analyticsEventHandler.send(Basic.ButtonSupport(source = AnalyticsParam.ScreensSources.TangemPay))
            sendFeedbackEmailUseCase(
                type = FeedbackEmailType.Visa.KycRejected(
                    walletMetaInfo = WalletMetaInfo(
                        userWalletId = stateHolder.getSelectedWalletId(),
                    ),
                    customerId = customerId,
                ),
            )
        }
    }

    override fun onIssuingCardClicked() {
        val issuingBottomSheet = bottomSheetMessage {
            infoBlock {
                icon(com.tangem.core.ui.R.drawable.ic_clock_24) {
                    type = MessageBottomSheetUM.Icon.Type.Informative
                    backgroundType = MessageBottomSheetUM.Icon.BackgroundType.Informative
                }
                title = resourceReference(R.string.tangempay_issuing_your_card)
                body = resourceReference(R.string.tangempay_issuing_your_card_description)
            }
            secondaryButton {
                text = resourceReference(R.string.common_got_it)
                onClick { closeBs() }
            }
        }

        uiMessageSender.send(issuingBottomSheet)
    }

    override fun onIssuingFailedClicked(customerId: String) {
        val issuingBottomSheet = bottomSheetMessage {
            infoBlock {
                icon(com.tangem.core.ui.R.drawable.ic_alert_24) {
                    type = MessageBottomSheetUM.Icon.Type.Warning
                    backgroundType = MessageBottomSheetUM.Icon.BackgroundType.Warning
                }
                title = resourceReference(R.string.tangempay_failed_to_issue_card)
                body = resourceReference(R.string.tangempay_failed_to_issue_card_support_description)
            }
            secondaryButton {
                text = resourceReference(R.string.tangempay_go_to_support)
                onClick {
                    onPaySupportClick(customerId)
                    closeBs()
                }
            }
        }

        uiMessageSender.send(issuingBottomSheet)
    }

    override fun onPaySupportClick(customerId: String) {
        modelScope.launch {
            val cardInfo = getWalletMetainfoUseCase.invoke(
                userWalletId = stateHolder.getSelectedWalletId(),
            ).getOrNull() ?: return@launch

            analyticsEventHandler.send(Basic.ButtonSupport(source = AnalyticsParam.ScreensSources.TangemPay))
            sendFeedbackEmailUseCase(
                FeedbackEmailType.Visa.FailedIssueCard(
                    walletMetaInfo = cardInfo,
                    customerId = customerId,
                ),
            )
        }
    }

    override fun onOnboardingBannerClick(userWalletId: UserWalletId) {
        modelScope.launch {
            analyticsEventHandler.send(TangemPayAnalyticsEvents.MainVisaPermanentBannerClicked())
            val isEligible = tangemPayEligibilityManager.getTangemPayAvailability(TangemPayEntryPoint.BANNER)
            if (isEligible) {
                router.openTangemPayOnboarding(mode = AppRoute.TangemPayOnboarding.Mode.FromBannerOnMain)
            } else {
                stateHolder.update(transformer = TangemPayHideOnboardingStateTransformer(userWalletId))
            }
        }
    }

    override fun onOnboardingBannerCloseClick(userWalletId: UserWalletId) {
        modelScope.launch {
            stateHolder.update(transformer = TangemPayHideOnboardingStateTransformer(userWalletId))
            onboardingRepository.setHideMainOnboardingBanner(userWalletId)
        }
    }

    private fun disableTangemPay(userWalletId: UserWalletId) {
        analyticsEventHandler.send(TangemPayAnalyticsEvents.KycCancelled())
        modelScope.launch {
            tangemPayOnboardingRepository.disableTangemPay(userWalletId)
                .onRight {
                    tangemPayMainScreenCustomerInfoUseCase.fetch(userWalletId)
                    paymentAccountStatusFetcher.invoke(PaymentAccountStatusFetcher.Params(userWalletId))
                }
                .onLeft { uiMessageSender.send(ToastMessage(resourceReference(R.string.common_something_went_wrong))) }
        }
    }
}