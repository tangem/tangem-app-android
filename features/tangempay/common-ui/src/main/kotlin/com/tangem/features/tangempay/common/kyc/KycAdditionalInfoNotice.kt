package com.tangem.features.tangempay.common.kyc

import com.tangem.core.analytics.api.AnalyticsEventHandler
import com.tangem.core.decompose.ui.UiMessageSender
import com.tangem.core.ui.R
import com.tangem.core.ui.ds2.modalstatus.TangemModalStatus
import com.tangem.core.ui.extensions.resourceReference
import com.tangem.core.ui.message.ModalMessage
import com.tangem.core.ui.message.modalMessage
import com.tangem.core.ui.res.generated.icons.Icons
import com.tangem.core.ui.res.generated.icons.ic_lightning_24
import com.tangem.domain.tangempay.TangemPayKycAdditionalInfoAnalyticsEvents.AccountType
import com.tangem.domain.tangempay.TangemPayKycAdditionalInfoAnalyticsEvents.CloseMethod
import com.tangem.domain.tangempay.TangemPayKycAdditionalInfoAnalyticsEvents.ContactSupportClicked
import com.tangem.domain.tangempay.TangemPayKycAdditionalInfoAnalyticsEvents.EntryPoint
import com.tangem.domain.tangempay.TangemPayKycAdditionalInfoAnalyticsEvents.NoticeShown
import com.tangem.domain.tangempay.TangemPayKycAdditionalInfoAnalyticsEvents.Placement
import com.tangem.domain.tangempay.TangemPayKycAdditionalInfoAnalyticsEvents.PopupClosed
import com.tangem.domain.tangempay.TangemPayKycAdditionalInfoAnalyticsEvents.PopupShown
import com.tangem.domain.tangempay.TangemPayKycAdditionalInfoAnalyticsEvents.ProvideInformationClicked
import com.tangem.domain.tangempay.TangemPayKycAdditionalInfoAnalyticsEvents.Source
import java.util.concurrent.atomic.AtomicBoolean

class KycAdditionalInfoNotice(
    private val accountType: AccountType,
    private val analytics: AnalyticsEventHandler,
    private val uiMessageSender: UiMessageSender,
    private val onProvideInformation: () -> Unit,
    private val onContactSupport: () -> Unit,
) {

    private var isRequired: Boolean = false

    fun onStatus(isRequired: Boolean, isPopupAllowed: Boolean) {
        val hasAppeared = isRequired && !this.isRequired
        this.isRequired = isRequired
        if (!hasAppeared) return

        analytics.send(NoticeShown(accountType = accountType, placement = Placement.ACCOUNT_BLOCK))
        if (isPopupAllowed) showPopup()
    }

    fun onBannerProvideInformationClick() {
        analytics.send(
            ProvideInformationClicked(
                accountType = accountType,
                source = Source.ACCOUNT_BLOCK,
                entryPoint = EntryPoint.ACCOUNT_BLOCK,
            ),
        )
        onProvideInformation()
    }

    private fun showPopup() {
        analytics.send(PopupShown(accountType = accountType, entryPoint = EntryPoint.ACCOUNT_ENTRY))
        uiMessageSender.send(createPopup())
    }

    private fun createPopup(): ModalMessage {
        val isResolved = AtomicBoolean(false)

        return modalMessage {
            status {
                variant = TangemModalStatus.Variant.Warning
                glyph(Icons.ic_lightning_24)
                title = resourceReference(R.string.tangempay_kyc_additional_info_title)
                description = resourceReference(R.string.tangempay_kyc_additional_info_popup_description)
            }
            secondaryButton {
                text = resourceReference(R.string.common_contact_support)
                onClick {
                    if (isResolved.compareAndSet(false, true)) {
                        analytics.send(
                            ContactSupportClicked(accountType = accountType, entryPoint = EntryPoint.ACCOUNT_ENTRY),
                        )
                        onContactSupport()
                        dismiss()
                    }
                }
            }
            primaryButton {
                text = resourceReference(R.string.tangempay_kyc_additional_info_button)
                onClick {
                    if (isResolved.compareAndSet(false, true)) {
                        analytics.send(
                            ProvideInformationClicked(
                                accountType = accountType,
                                source = Source.POPUP,
                                entryPoint = EntryPoint.ACCOUNT_ENTRY,
                            ),
                        )
                        onProvideInformation()
                        dismiss()
                    }
                }
            }
            onDismiss {
                if (isResolved.compareAndSet(false, true)) {
                    analytics.send(
                        PopupClosed(
                            accountType = accountType,
                            entryPoint = EntryPoint.ACCOUNT_ENTRY,
                            closeMethod = CloseMethod.UNKNOWN,
                        ),
                    )
                }
            }
        }
    }
}