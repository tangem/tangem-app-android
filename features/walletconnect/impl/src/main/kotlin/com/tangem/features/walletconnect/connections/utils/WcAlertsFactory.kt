package com.tangem.features.walletconnect.connections.utils

import com.tangem.core.ui.R
import com.tangem.core.ui.components.bottomsheets.message.*
import com.tangem.core.ui.components.bottomsheets.message.MessageBottomSheetUMV2.Icon.Type
import com.tangem.core.ui.extensions.TextReference
import com.tangem.core.ui.extensions.resourceReference
import com.tangem.core.ui.extensions.wrappedList
import com.tangem.features.walletconnect.transaction.routes.WcTransactionRoutes

internal object WcAlertsFactory {

    fun createCommonTransactionAppInfoAlertUM(alertType: WcTransactionRoutes.Alert.Type) = when (alertType) {
        is WcTransactionRoutes.Alert.Type.Verified ->
            createVerifiedDomainAlert(alertType.appName)
        is WcTransactionRoutes.Alert.Type.UnknownDomain ->
            createUnknownDomainAlert()
        is WcTransactionRoutes.Alert.Type.UnsafeDomain ->
            createUnsafeDomainAlert()
        is WcTransactionRoutes.Alert.Type.MaliciousInfo ->
            createMaliciousDAppAlert(alertType.description, alertType.onClick)
        is WcTransactionRoutes.Alert.Type.UnknownError ->
            createUnknownErrorAlert(alertType.errorMessage, alertType.onDismiss)
    }

    fun createUnknownDomainAlert(activeButtonOnClick: (() -> Unit)? = null): MessageBottomSheetUMV2 {
        return messageBottomSheetUM {
            infoBlock {
                icon(R.drawable.img_knight_shield_32) {
                    backgroundType = MessageBottomSheetUMV2.Icon.BackgroundType.Attention
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
            if (activeButtonOnClick != null) {
                secondaryButton {
                    text = resourceReference(R.string.wc_alert_connect_anyway)
                    onClick { activeButtonOnClick() }
                }
            }
        }
    }

    fun createUnsafeDomainAlert(activeButtonOnClick: (() -> Unit)? = null): MessageBottomSheetUMV2 {
        return messageBottomSheetUM {
            infoBlock {
                icon(R.drawable.img_knight_shield_32) {
                    type = Type.Warning
                    backgroundType = MessageBottomSheetUMV2.Icon.BackgroundType.Warning
                }
                title = resourceReference(R.string.security_alert_title)
                body = resourceReference(R.string.wc_alert_domain_issues_description)
                chip(resourceReference(R.string.wc_alert_audit_malicious_domain)) {
                    type = MessageBottomSheetUMV2.Chip.Type.Warning
                }
            }
            primaryButton {
                text = resourceReference(R.string.common_cancel)
                onClick { closeBs() }
            }
            if (activeButtonOnClick != null) {
                secondaryButton {
                    text = resourceReference(R.string.wc_alert_connect_anyway)
                    onClick { activeButtonOnClick() }
                }
            }
        }
    }

    fun createUnsupportedDomainAlert(appName: String): MessageBottomSheetUMV2 {
        return messageBottomSheetUM {
            infoBlock {
                icon(R.drawable.ic_wallet_connect_24) {
                    type = MessageBottomSheetUMV2.Icon.Type.Informative
                    backgroundType = MessageBottomSheetUMV2.Icon.BackgroundType.SameAsTint
                }
                title = resourceReference(R.string.wc_alert_unsupported_dapps_title)
                body = resourceReference(R.string.wc_alert_unsupported_dapps_description, wrappedList(appName))
            }
            primaryButton {
                text = resourceReference(R.string.common_got_it)
                onClick { closeBs() }
            }
        }
    }

    fun createUnsupportedChainAlert(appName: String): MessageBottomSheetUMV2 {
        return messageBottomSheetUM {
            infoBlock {
                icon(R.drawable.ic_network_new_24) {
                    type = MessageBottomSheetUMV2.Icon.Type.Informative
                    backgroundType = MessageBottomSheetUMV2.Icon.BackgroundType.SameAsTint
                }
                title = resourceReference(R.string.wc_alert_unsupported_networks_title)
                body = resourceReference(R.string.wc_alert_unsupported_networks_description, wrappedList(appName))
            }
            primaryButton {
                text = resourceReference(R.string.common_got_it)
                onClick { closeBs() }
            }
        }
    }

    private fun createUnknownErrorAlert(errorMessage: String?, onDismiss: () -> Unit): MessageBottomSheetUMV2 {
        return messageBottomSheetUM {
            infoBlock {
                icon(R.drawable.img_attention_20) {
                    backgroundType = MessageBottomSheetUMV2.Icon.BackgroundType.Attention
                }
                title = resourceReference(R.string.wc_alert_unknown_error_title)
                body = if (errorMessage.isNullOrEmpty()) {
                    resourceReference(R.string.wc_alert_unknown_error_description_no_error_code)
                } else {
                    resourceReference(
                        R.string.wc_alert_unknown_error_description,
                        wrappedList(errorMessage),
                    )
                }
            }
            secondaryButton {
                text = resourceReference(R.string.balance_hidden_got_it_button)
                onClick { onDismiss() }
            }
        }
    }

    private fun createMaliciousDAppAlert(
        description: String?,
        activeButtonOnClick: (() -> Unit),
    ): MessageBottomSheetUMV2 {
        return messageBottomSheetUM {
            infoBlock {
                icon(R.drawable.img_knight_shield_32) {
                    backgroundType = MessageBottomSheetUMV2.Icon.BackgroundType.Attention
                }
                title = resourceReference(R.string.security_alert_title)
                if (!description.isNullOrEmpty()) {
                    body = TextReference.Str(description)
                }
            }
            primaryButton {
                text = resourceReference(R.string.common_cancel)
                onClick { closeBs() }
            }
            secondaryButton {
                text = resourceReference(R.string.wc_alert_sign_anyway)
                onClick { activeButtonOnClick() }
            }
        }
    }

    fun createVerifiedDomainAlert(appName: String): MessageBottomSheetUMV2 {
        return messageBottomSheetUM {
            infoBlock {
                icon(R.drawable.img_approvale2_20) {
                    backgroundType = MessageBottomSheetUMV2.Icon.BackgroundType.Accent
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