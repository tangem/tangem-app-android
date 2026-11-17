package com.tangem.features.jointaccount.main.model

import com.tangem.core.analytics.api.AnalyticsEventHandler
import com.tangem.core.analytics.models.AnalyticsParam
import com.tangem.core.decompose.di.ModelScoped
import com.tangem.core.decompose.ui.UiMessageSender
import com.tangem.core.navigation.notifications.SystemNotificationsStateProvider
import com.tangem.core.navigation.settings.SettingsManager
import com.tangem.core.ui.extensions.resourceReference
import com.tangem.core.ui.message.DialogMessage
import com.tangem.core.ui.message.EventMessageAction
import com.tangem.domain.models.wallet.UserWalletId
import com.tangem.domain.notifications.GetIsHuaweiDeviceWithoutGoogleServicesUseCase
import com.tangem.domain.notifications.SetShouldShowNotificationUseCase
import com.tangem.domain.notifications.ShouldShowNotificationUseCase
import com.tangem.features.pushnotifications.api.analytics.PushNotificationAnalyticEvents
import com.tangem.features.pushnotifications.api.utils.getPushPermissionOrNull
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.receiveAsFlow
import javax.inject.Inject
import com.tangem.core.ui.R as CoreUiR

internal enum class PushPermissionResult { Granted, Denied, Blocked }

@Suppress("LongParameterList")
@ModelScoped
internal class JointAccountPushPermissionPrompt @Inject constructor(
    private val systemNotificationsStateProvider: SystemNotificationsStateProvider,
    private val getIsHuaweiDeviceWithoutGoogleServicesUseCase: GetIsHuaweiDeviceWithoutGoogleServicesUseCase,
    private val shouldShowNotificationUseCase: ShouldShowNotificationUseCase,
    private val setShouldShowNotificationUseCase: SetShouldShowNotificationUseCase,
    private val settingsManager: SettingsManager,
    private val uiMessageSender: UiMessageSender,
    private val analyticsEventHandler: AnalyticsEventHandler,
) {

    private val systemRequestChannel = Channel<Unit>(Channel.BUFFERED)

    val systemRequests: Flow<Unit> = systemRequestChannel.receiveAsFlow()

    suspend fun promptOnce(userWalletId: UserWalletId, cryptoAccountId: String) {
        if (systemNotificationsStateProvider.areNotificationsEnabled()) return
        if (getIsHuaweiDeviceWithoutGoogleServicesUseCase()) return

        val key = "$PROMPT_KEY_PREFIX${userWalletId.stringValue}_$cryptoAccountId"
        if (!shouldShowNotificationUseCase(key)) return
        setShouldShowNotificationUseCase(key = key, value = false)

        if (getPushPermissionOrNull() == null) {
            showEnableNotificationsDialog()
        } else {
            systemRequestChannel.send(Unit)
        }
    }

    fun onSystemRequestResult(result: PushPermissionResult) {
        analyticsEventHandler.send(
            PushNotificationAnalyticEvents.PermissionStatus(
                isAllowed = result == PushPermissionResult.Granted,
                source = AnalyticsParam.ScreensSources.JointAccount,
            ),
        )
        val isBlockedInSystem = result == PushPermissionResult.Granted &&
            !systemNotificationsStateProvider.areNotificationsEnabled()
        if (result == PushPermissionResult.Blocked || isBlockedInSystem) showEnableNotificationsDialog()
    }

    private fun showEnableNotificationsDialog() {
        uiMessageSender.send(
            DialogMessage(
                title = resourceReference(CoreUiR.string.push_notifications_permission_alert_title),
                message = resourceReference(CoreUiR.string.push_notifications_permission_alert_description),
                firstAction = EventMessageAction(
                    title = resourceReference(CoreUiR.string.push_notifications_permission_alert_positive_button),
                    onClick = settingsManager::openAppNotificationSettings,
                ),
                secondAction = EventMessageAction(
                    title = resourceReference(CoreUiR.string.push_notifications_permission_alert_negative_button),
                    onClick = {},
                ),
            ),
        )
    }

    private companion object {
        const val PROMPT_KEY_PREFIX = "joint_account_push_prompt_"
    }
}