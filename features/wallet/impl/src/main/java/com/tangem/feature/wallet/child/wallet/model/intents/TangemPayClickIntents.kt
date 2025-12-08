package com.tangem.feature.wallet.child.wallet.model.intents

import com.tangem.core.decompose.di.ModelScoped
import com.tangem.core.decompose.ui.UiMessageSender
import com.tangem.core.res.R
import com.tangem.core.ui.components.bottomsheets.message.*
import com.tangem.core.ui.extensions.resourceReference
import com.tangem.core.ui.message.bottomSheetMessage
import com.tangem.domain.feedback.GetWalletMetaInfoUseCase
import com.tangem.domain.feedback.SendFeedbackEmailUseCase
import com.tangem.domain.feedback.models.FeedbackEmailType
import com.tangem.domain.models.wallet.UserWalletId
import com.tangem.domain.models.wallet.requireColdWallet
import com.tangem.domain.pay.repository.OnboardingRepository
import com.tangem.domain.pay.usecase.ProduceTangemPayInitialDataUseCase
import com.tangem.domain.pay.usecase.TangemPayMainScreenCustomerInfoUseCase
import com.tangem.domain.wallets.usecase.GetUserWalletUseCase
import com.tangem.feature.wallet.presentation.wallet.state.WalletStateController
import com.tangem.features.tangempay.TangemPayFeatureToggles
import kotlinx.coroutines.launch
import javax.inject.Inject

internal interface TangemPayIntents {

    suspend fun onPullToRefresh()

    fun onRefreshPayToken(userWalletId: UserWalletId)

    fun onIssuingCardClicked()

    fun onIssuingFailedClicked()

    fun onPaySupportClick()
}

@Suppress("LongParameterList")
@ModelScoped
internal class TangemPayClickIntentsImplementor @Inject constructor(
    private val stateHolder: WalletStateController,
    private val featureToggles: TangemPayFeatureToggles,
    private val onboardingRepository: OnboardingRepository,
    private val produceInitialDataTangemPay: ProduceTangemPayInitialDataUseCase,
    private val getWalletMetainfoUseCase: GetWalletMetaInfoUseCase,
    private val getUserWalletUseCase: GetUserWalletUseCase,
    private val sendFeedbackEmailUseCase: SendFeedbackEmailUseCase,
    private val tangemPayMainScreenCustomerInfoUseCase: TangemPayMainScreenCustomerInfoUseCase,
    private val uiMessageSender: UiMessageSender,
) : BaseWalletClickIntents(), TangemPayIntents {

    override suspend fun onPullToRefresh() {
        val userWalletId = stateHolder.getSelectedWalletId()
        if (!featureToggles.isTangemPayEnabled ||
            !onboardingRepository.isTangemPayInitialDataProduced(userWalletId)
        ) {
            return
        }
        tangemPayMainScreenCustomerInfoUseCase.fetch(userWalletId)
    }

    override fun onRefreshPayToken(userWalletId: UserWalletId) {
        modelScope.launch {
            produceInitialDataTangemPay.invoke(userWalletId)
            tangemPayMainScreenCustomerInfoUseCase.fetch(userWalletId)
        }
    }

    override fun onIssuingCardClicked() {
        val issuingBottomSheet = bottomSheetMessage {
            infoBlock {
                icon(com.tangem.core.ui.R.drawable.ic_clock_24) {
                    backgroundType = MessageBottomSheetUMV2.Icon.BackgroundType.Informative
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

    override fun onIssuingFailedClicked() {
        val issuingBottomSheet = bottomSheetMessage {
            infoBlock {
                icon(com.tangem.core.ui.R.drawable.ic_alert_24) {
                    type = MessageBottomSheetUMV2.Icon.Type.Warning
                    backgroundType = MessageBottomSheetUMV2.Icon.BackgroundType.Warning
                }
                title = resourceReference(R.string.tangempay_failed_to_issue_card)
                body = resourceReference(R.string.tangempay_failed_to_issue_card_support_description)
            }
            secondaryButton {
                text = resourceReference(R.string.details_row_title_contact_to_support)
                onClick {
                    onPaySupportClick()
                    closeBs()
                }
            }
        }

        uiMessageSender.send(issuingBottomSheet)
    }

    override fun onPaySupportClick() {
        modelScope.launch {
            val userWalletId = stateHolder.getSelectedWalletId()
            val userWallet = getUserWalletUseCase.invoke(userWalletId).getOrNull() ?: return@launch
            val cardInfo = getWalletMetainfoUseCase.invoke(
                userWallet.requireColdWallet().scanResponse,
            ).getOrNull() ?: return@launch

            sendFeedbackEmailUseCase(
                FeedbackEmailType.Visa.FailedIssueCard(
                    walletMetaInfo = cardInfo,
                ),
            )
        }
    }
}