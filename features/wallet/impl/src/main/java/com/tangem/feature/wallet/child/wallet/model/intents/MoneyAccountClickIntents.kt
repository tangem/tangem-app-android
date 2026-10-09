package com.tangem.feature.wallet.child.wallet.model.intents

import androidx.compose.ui.text.SpanStyle
import com.arkivanov.decompose.router.slot.activate
import com.tangem.common.routing.AppRoute
import com.tangem.core.decompose.di.ModelScoped
import com.tangem.core.decompose.ui.UiMessageSender
import com.tangem.core.res.R
import com.tangem.core.ui.components.bottomsheets.message.*
import com.tangem.core.ui.extensions.plus
import com.tangem.core.ui.extensions.resourceReference
import com.tangem.core.ui.extensions.stringReference
import com.tangem.core.ui.extensions.styledResourceReference
import com.tangem.core.ui.message.bottomSheetMessage
import com.tangem.core.ui.res.TangemTheme
import com.tangem.core.ui.res.generated.icons.Icons
import com.tangem.core.ui.res.generated.icons.ic_clock_28
import com.tangem.core.ui.res.generated.icons.ic_heart_broken_28
import com.tangem.domain.feedback.SendFeedbackEmailUseCase
import com.tangem.domain.feedback.models.FeedbackEmailType
import com.tangem.domain.feedback.models.WalletMetaInfo
import com.tangem.domain.models.pay.TangemPayProduct
import com.tangem.domain.models.wallet.UserWalletId
import com.tangem.domain.moneyaccount.repository.MoneyAccountRepository
import com.tangem.feature.wallet.presentation.wallet.state.WalletStateController
import com.tangem.feature.wallet.presentation.wallet.state.model.WalletDialogConfig
import com.tangem.feature.wallet.presentation.wallet.state.transformers.MoneyAccountHideStateTransformer
import kotlinx.coroutines.launch
import javax.inject.Inject

internal interface MoneyAccountIntents {

    fun onMoneyAccountActivatingClicked()

    fun onMoneyAccountKycRequiredClicked(userWalletId: UserWalletId, customerId: String?)

    fun onMoneyAccountVerificationClicked(userWalletId: UserWalletId, customerId: String)

    fun onMoneyAccountVerificationDeclinedClicked(userWalletId: UserWalletId, customerId: String)

    fun onMoneyAccountCountryNotSupportedClicked(userWalletId: UserWalletId, countryCode: String?)
}

@ModelScoped
internal class MoneyAccountClickIntentsImplementor @Inject constructor(
    private val uiMessageSender: UiMessageSender,
    private val sendFeedbackEmailUseCase: SendFeedbackEmailUseCase,
    private val moneyAccountRepository: MoneyAccountRepository,
    private val stateHolder: WalletStateController,
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

    override fun onMoneyAccountKycRequiredClicked(userWalletId: UserWalletId, customerId: String?) {
        val bottomSheet = kycRequiredBottomSheet(
            onContactSupportClick = { sendFeedbackEmail(kycRequiredFeedbackEmailType(userWalletId, customerId)) },
            onContinueVerificationClick = {
                router.openTangemPayEligibility(product = TangemPayProduct.BANKING_ACCOUNT, userWalletId = userWalletId)
            },
        )

        uiMessageSender.send(bottomSheet)
    }

    override fun onMoneyAccountVerificationClicked(userWalletId: UserWalletId, customerId: String) {
        val bottomSheet = bottomSheetMessage {
            infoBlock {
                vector(Icons.ic_clock_28) {
                    type = MessageBottomSheetUM.Vector.Type.Informative
                    backgroundType = MessageBottomSheetUM.Vector.BackgroundType.Informative
                }
                title = resourceReference(R.string.money_account_main_status_dialog_verification_title)
                body = resourceReference(R.string.money_account_main_status_dialog_verification_description)
            }
            secondaryButton {
                text = resourceReference(R.string.common_contact_support)
                onClick {
                    sendFeedbackEmail(
                        FeedbackEmailType.Visa.DirectUserRequest(
                            walletMetaInfo = WalletMetaInfo(userWalletId = userWalletId),
                            customerId = customerId,
                        ),
                    )
                    closeBs()
                }
            }
            primaryButton {
                text = resourceReference(R.string.money_account_main_status_dialog_view_status)
                onClick {
                    router.openTangemPayOnboarding(AppRoute.TangemPayOnboarding.Mode.ContinueOnboarding(userWalletId))
                    closeBs()
                }
            }
        }

        uiMessageSender.send(bottomSheet)
    }

    override fun onMoneyAccountVerificationDeclinedClicked(userWalletId: UserWalletId, customerId: String) {
        val bottomSheet = bottomSheetMessage {
            infoBlock {
                vector(Icons.ic_heart_broken_28) {
                    type = MessageBottomSheetUM.Vector.Type.Warning
                    backgroundType = MessageBottomSheetUM.Vector.BackgroundType.Warning
                }
                title = resourceReference(R.string.money_account_main_status_dialog_verification_declined_title)
                body = resourceReference(R.string.money_account_main_status_dialog_verification_declined_description)
            }
            secondaryButton {
                text = resourceReference(R.string.common_contact_support)
                onClick {
                    sendFeedbackEmail(
                        FeedbackEmailType.Visa.KycRejected(
                            walletMetaInfo = WalletMetaInfo(userWalletId = userWalletId),
                            customerId = customerId,
                        ),
                    )
                    closeBs()
                }
            }
            primaryButton {
                text = resourceReference(R.string.money_account_main_status_dialog_hide_account)
                onClick {
                    hideMoneyAccount(userWalletId)
                    closeBs()
                }
            }
        }

        uiMessageSender.send(bottomSheet)
    }

    override fun onMoneyAccountCountryNotSupportedClicked(userWalletId: UserWalletId, countryCode: String?) {
        val contactSupportLink = styledResourceReference(
            id = R.string.common_contact_support,
            spanStyleReference = { SpanStyle(color = TangemTheme.colors2.text.neutral.primary) },
            onClick = {
                sendFeedbackEmail(
                    FeedbackEmailType.DirectUserRequest(walletMetaInfo = WalletMetaInfo(userWalletId = userWalletId)),
                )
            },
        )
        val bottomSheet = bottomSheetMessage {
            infoBlock {
                vector(Icons.ic_heart_broken_28) {
                    type = MessageBottomSheetUM.Vector.Type.Warning
                    backgroundType = MessageBottomSheetUM.Vector.BackgroundType.Warning
                }
                title = resourceReference(R.string.money_account_main_status_dialog_country_not_supported_title)
                body = resourceReference(R.string.money_account_main_status_dialog_country_not_supported_description) +
                    stringReference(" ") +
                    contactSupportLink
            }
            if (countryCode != null) {
                secondaryButton {
                    text = resourceReference(R.string.money_account_main_status_dialog_join_waitlist)
                    onClick {
                        router.dialogNavigation.activate(
                            configuration = WalletDialogConfig.TangemPayWaitlist(
                                userWalletId = userWalletId,
                                countryCode = countryCode,
                            ),
                        )
                        closeBs()
                    }
                }
            }
            primaryButton {
                text = resourceReference(R.string.money_account_main_status_dialog_hide_account)
                onClick {
                    hideMoneyAccount(userWalletId)
                    closeBs()
                }
            }
        }

        uiMessageSender.send(bottomSheet)
    }

    private fun hideMoneyAccount(userWalletId: UserWalletId) {
        stateHolder.update(transformer = MoneyAccountHideStateTransformer(userWalletId))
        modelScope.launch { moneyAccountRepository.hideMainBlock(userWalletId) }
    }

    private fun sendFeedbackEmail(type: FeedbackEmailType) {
        modelScope.launch { sendFeedbackEmailUseCase(type = type) }
    }
}