package com.tangem.feature.wallet.child.wallet.model.intents

import com.tangem.core.analytics.api.AnalyticsEventHandler
import com.tangem.core.analytics.models.AnalyticsParam
import com.tangem.core.analytics.models.Basic
import com.tangem.core.decompose.di.ModelScoped
import com.tangem.core.decompose.ui.UiMessageSender
import com.tangem.core.res.R
import com.tangem.core.ui.components.bottomsheets.message.*
import com.tangem.core.ui.extensions.resourceReference
import com.tangem.core.ui.ds2.modalstatus.TangemModalStatus
import com.tangem.core.ui.message.bottomSheetMessage
import com.tangem.core.ui.message.modalMessage
import com.tangem.core.ui.res.generated.icons.Icons
import com.tangem.core.ui.res.generated.icons.ic_clock_28
import com.tangem.core.ui.res.generated.icons.ic_heart_broken_28
import com.tangem.domain.feedback.SendFeedbackEmailUseCase
import com.tangem.domain.feedback.models.FeedbackEmailType
import com.tangem.domain.feedback.models.WalletMetaInfo
import com.tangem.domain.models.wallet.UserWalletId
import kotlinx.coroutines.launch
import javax.inject.Inject

internal interface MoneyAccountIntents {

    fun onMoneyAccountActivatingClicked()

    fun onMoneyAccountFailedClicked(userWalletId: UserWalletId, customerId: String)

    fun onMoneyAccountClicked(userWalletId: UserWalletId)
}

@ModelScoped
internal class MoneyAccountClickIntentsImplementor @Inject constructor(
    private val uiMessageSender: UiMessageSender,
    private val sendFeedbackEmailUseCase: SendFeedbackEmailUseCase,
    private val analyticsEventHandler: AnalyticsEventHandler,
) : BaseWalletClickIntents(), MoneyAccountIntents {

    override fun onMoneyAccountActivatingClicked() {
        val bottomSheet = bottomSheetMessage {
            infoBlock {
                vector(Icons.ic_clock_28) {
                    type = MessageBottomSheetUM.Vector.Type.Informative
                    backgroundType = MessageBottomSheetUM.Vector.BackgroundType.Informative
                }
                title = resourceReference(R.string.money_account_main_status_dialog_activating_title)
                body = resourceReference(R.string.money_account_main_status_dialog_activating_description)
            }
            secondaryButton {
                text = resourceReference(R.string.common_got_it)
                onClick { closeBs() }
            }
        }

        uiMessageSender.send(bottomSheet)
    }

    override fun onMoneyAccountFailedClicked(userWalletId: UserWalletId, customerId: String) {
        val modal = modalMessage {
            status {
                variant = TangemModalStatus.Variant.Error
                title = resourceReference(R.string.money_account_main_status_dialog_failed_title)
                description = resourceReference(R.string.money_account_main_status_dialog_failed_description)
                glyph(Icons.ic_heart_broken_28)
            }
            primaryButton {
                text = resourceReference(R.string.common_contact_support)
                onClick {
                    contactSupport(userWalletId = userWalletId, customerId = customerId)
                    dismiss()
                }
            }
        }

        uiMessageSender.send(modal)
    }

    private fun contactSupport(userWalletId: UserWalletId, customerId: String) {
        analyticsEventHandler.send(Basic.ButtonSupport(source = AnalyticsParam.ScreensSources.TangemPay))
        modelScope.launch {
            sendFeedbackEmailUseCase(
                type = FeedbackEmailType.Visa.DirectUserRequest(
                    walletMetaInfo = WalletMetaInfo(userWalletId = userWalletId),
                    customerId = customerId,
                ),
            )
        }
    }

    override fun onMoneyAccountClicked(userWalletId: UserWalletId) {
        router.openMoneyAccountDetails(userWalletId = userWalletId)
    }
}