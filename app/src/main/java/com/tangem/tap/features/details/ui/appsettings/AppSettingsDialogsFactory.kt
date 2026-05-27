package com.tangem.tap.features.details.ui.appsettings

import com.tangem.core.ui.extensions.resourceReference
import com.tangem.core.ui.extensions.wrappedList
import com.tangem.core.ui.message.DialogMessage
import com.tangem.core.ui.message.EventMessageAction
import com.tangem.wallet.R

internal class AppSettingsDialogsFactory {

    fun createDisableBiometricAuthenticationAlert(onDisable: () -> Unit): DialogMessage {
        return DialogMessage(
            title = resourceReference(R.string.common_attention),
            message = resourceReference(
                R.string.app_settings_off_biometrics_alert_message,
                wrappedList(resourceReference(R.string.common_biometrics)),
            ),
            isDismissable = false,
            firstActionBuilder = {
                EventMessageAction(
                    title = resourceReference(R.string.common_disable),
                    isWarning = true,
                    onClick = onDisable,
                )
            },
            secondActionBuilder = { cancelAction() },
        )
    }

    fun createEnableRequireAccessCodeAlert(onEnable: () -> Unit): DialogMessage {
        return DialogMessage(
            title = resourceReference(R.string.common_attention),
            message = resourceReference(R.string.app_settings_on_require_access_code_alert_message),
            isDismissable = false,
            firstActionBuilder = {
                EventMessageAction(
                    title = resourceReference(R.string.common_enable),
                    onClick = onEnable,
                )
            },
            secondActionBuilder = { cancelAction() },
        )
    }

    fun createDisableRequireAccessCodeAlert(onDisable: () -> Unit): DialogMessage {
        return DialogMessage(
            title = resourceReference(R.string.common_attention),
            message = resourceReference(R.string.app_settings_off_require_access_code_alert_message),
            isDismissable = false,
            firstActionBuilder = {
                EventMessageAction(
                    title = resourceReference(R.string.common_disable),
                    isWarning = true,
                    onClick = onDisable,
                )
            },
            secondActionBuilder = { cancelAction() },
        )
    }
}