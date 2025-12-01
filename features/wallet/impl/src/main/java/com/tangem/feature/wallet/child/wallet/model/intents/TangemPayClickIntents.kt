package com.tangem.feature.wallet.child.wallet.model.intents

import com.tangem.core.decompose.di.ModelScoped
import com.tangem.core.decompose.ui.UiMessageSender
import com.tangem.core.res.R
import com.tangem.core.ui.components.bottomsheets.message.MessageBottomSheetUMV2
import com.tangem.core.ui.extensions.resourceReference
import com.tangem.core.ui.message.BottomSheetMessageV2
import com.tangem.domain.feedback.GetWalletMetaInfoUseCase
import com.tangem.domain.feedback.SendFeedbackEmailUseCase
import com.tangem.domain.feedback.models.FeedbackEmailType
import com.tangem.domain.models.wallet.UserWalletId
import com.tangem.domain.models.wallet.requireColdWallet
import com.tangem.domain.pay.repository.OnboardingRepository
import com.tangem.domain.pay.usecase.ProduceTangemPayInitialDataUseCase
import com.tangem.domain.wallets.usecase.GetUserWalletUseCase
import com.tangem.feature.wallet.child.wallet.model.TangemPayMainInfoManager
import com.tangem.feature.wallet.presentation.wallet.state.WalletStateController
import com.tangem.features.tangempay.TangemPayFeatureToggles
import kotlinx.collections.immutable.persistentListOf
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
    private val tangemPayInfoManager: TangemPayMainInfoManager,
    private val uiMessageSender: UiMessageSender,
) : BaseWalletClickIntents(), TangemPayIntents {

    override suspend fun onPullToRefresh() {
        val userWalletId = stateHolder.getSelectedWalletId()
        if (!featureToggles.isTangemPayEnabled ||
            !onboardingRepository.isTangemPayInitialDataProduced(userWalletId)
        ) {
            return
        }
        tangemPayInfoManager.refreshTangemPayInfo(userWalletId)
    }

    override fun onRefreshPayToken(userWalletId: UserWalletId) {
        modelScope.launch {
            produceInitialDataTangemPay.invoke(userWalletId)
            tangemPayInfoManager.refreshTangemPayInfo(userWalletId)
        }
    }

    override fun onIssuingCardClicked() {
        val icon = MessageBottomSheetUMV2.Icon(
            res = com.tangem.core.ui.R.drawable.ic_clock_24,
            backgroundType = MessageBottomSheetUMV2.Icon.BackgroundType.Informative,
        )

        val info = MessageBottomSheetUMV2.InfoBlock(
            title = resourceReference(R.string.tangempay_issuing_your_card),
            body = resourceReference(R.string.tangempay_issuing_your_card_description),
        )

        val button = MessageBottomSheetUMV2.Button(
            text = resourceReference(R.string.common_got_it),
            onClick = { closeBs() },
        )

        val issuingBottomSheet = BottomSheetMessageV2(
            messageBottomSheetUMV2 = MessageBottomSheetUMV2(
                elements = persistentListOf(icon, info, button),
            ),
        )
        uiMessageSender.send(issuingBottomSheet)
    }

    override fun onIssuingFailedClicked() {
        val icon = MessageBottomSheetUMV2.Icon(
            res = com.tangem.core.ui.R.drawable.ic_alert_24,
            backgroundType = MessageBottomSheetUMV2.Icon.BackgroundType.Warning,
            type = MessageBottomSheetUMV2.Icon.Type.Warning,
        )

        val info = MessageBottomSheetUMV2.InfoBlock(
            title = resourceReference(R.string.tangempay_failed_to_issue_card),
            body = resourceReference(R.string.tangempay_failed_to_issue_card_support_description),
        )

        val button = MessageBottomSheetUMV2.Button(
            text = resourceReference(R.string.details_row_title_contact_to_support),
            onClick = {
                onPaySupportClick()
                closeBs()
            },
        )

        val issuingBottomSheet = BottomSheetMessageV2(
            messageBottomSheetUMV2 = MessageBottomSheetUMV2(
                elements = persistentListOf(icon, info, button),
            ),
        )
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