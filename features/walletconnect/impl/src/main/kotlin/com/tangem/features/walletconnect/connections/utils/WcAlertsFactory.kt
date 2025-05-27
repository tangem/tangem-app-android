package com.tangem.features.walletconnect.connections.utils

import com.tangem.core.ui.R
import com.tangem.core.ui.components.bottomsheets.message.*
import com.tangem.core.ui.extensions.resourceReference
import com.tangem.core.ui.extensions.wrappedList

internal object WcAlertsFactory {

    fun createUnknownDomainAlert(onClick: () -> Unit): MessageBottomSheetUMV2 {
        return messageBottomSheetUM {
            infoBlock {
                icon(R.drawable.img_knight_shield_32) {
                    type = MessageBottomSheetUMV2.Icon.Type.Attention
                    backgroundType = MessageBottomSheetUMV2.Icon.BackgroundType.SameAsTint
                }
                title = resourceReference(R.string.security_alert_title)
                body = resourceReference(R.string.wc_alert_domain_issues_description)
                chip(resourceReference(R.string.wc_alert_audit_unknown_domain)) {
                    type = MessageBottomSheetUMV2.Chip.Type.Unspecified
                }
            }
            primaryButton {
                text = resourceReference(R.string.common_cancel)
                onClick { closeBs() }
            }
            secondaryButton {
                text = resourceReference(R.string.wc_alert_connect_anyway)
                onClick { onClick() }
            }
        }
    }

    fun createUnsafeDomainAlert(onClick: () -> Unit): MessageBottomSheetUMV2 {
        return messageBottomSheetUM {
            infoBlock {
                icon(R.drawable.img_knight_shield_32) {
                    type = MessageBottomSheetUMV2.Icon.Type.Warning
                    backgroundType = MessageBottomSheetUMV2.Icon.BackgroundType.SameAsTint
                }
                title = resourceReference(R.string.security_alert_title)
                body = resourceReference(R.string.wc_alert_domain_issues_description)
                chip(resourceReference(R.string.wc_alert_audit_unknown_domain)) {
                    type = MessageBottomSheetUMV2.Chip.Type.Warning
                }
            }
            primaryButton {
                text = resourceReference(R.string.common_cancel)
                onClick { closeBs() }
            }
            secondaryButton {
                text = resourceReference(R.string.wc_alert_connect_anyway)
                onClick { onClick() }
            }
        }
    }

    fun createVerifiedDomainAlert(appName: String): MessageBottomSheetUMV2 {
        return messageBottomSheetUM {
            infoBlock {
                icon(R.drawable.img_approvale2_20) {
                    type = MessageBottomSheetUMV2.Icon.Type.Accent
                    backgroundType = MessageBottomSheetUMV2.Icon.BackgroundType.Unspecified
                }
                title = resourceReference(R.string.wc_alert_verified_domain_title)
                body = resourceReference(R.string.wc_alert_verified_domain_description, wrappedList(appName))
            }
            secondaryButton {
                text = resourceReference(R.string.common_done)
                onClick { closeBs() }
            }
        }
    }
}