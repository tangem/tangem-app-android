package com.tangem.feature.wallet.presentation.wallet.viewmodels.intents

import com.tangem.core.analytics.api.AnalyticsEventHandler
import com.tangem.core.analytics.models.AnalyticsParam
import com.tangem.domain.settings.NeverRequestPermissionUseCase
import com.tangem.features.pushnotifications.api.analytics.PushNotificationAnalyticEvents
import com.tangem.features.pushnotifications.api.utils.PUSH_PERMISSION
import dagger.hilt.android.scopes.ViewModelScoped
import kotlinx.coroutines.launch
import javax.inject.Inject

internal interface WalletPushPermissionClickIntents {

    fun onRequestPushPermission()

    fun onNeverAskPushPermission(isUserDismissed: Boolean)

    fun onDenyPushPermission()

    fun onAllowPushPermission()
}

@ViewModelScoped
internal class WalletPushPermissionClickIntentsImplementor @Inject constructor(
    private val neverRequestPermissionUseCase: NeverRequestPermissionUseCase,
    private val analyticsEventHandler: AnalyticsEventHandler,
) : BaseWalletClickIntents(), WalletPushPermissionClickIntents {

    private var isUserDismissedDialog: Boolean = true
    override fun onRequestPushPermission() {
        isUserDismissedDialog = false
        analyticsEventHandler.send(
            PushNotificationAnalyticEvents.ButtonAllow(AnalyticsParam.ScreensSources.Main),
        )
    }

    override fun onNeverAskPushPermission(isUserDismissed: Boolean) {
        if (isUserDismissedDialog != isUserDismissed) return
        viewModelScope.launch {
            analyticsEventHandler.send(
                PushNotificationAnalyticEvents.ButtonLater(AnalyticsParam.ScreensSources.Main),
            )
            neverRequestPermissionUseCase(PUSH_PERMISSION)
        }
    }

    override fun onDenyPushPermission() {
        analyticsEventHandler.send(PushNotificationAnalyticEvents.PermissionStatus(isAllowed = false))
        viewModelScope.launch {
            neverRequestPermissionUseCase(PUSH_PERMISSION)
        }
    }

    override fun onAllowPushPermission() {
        analyticsEventHandler.send(PushNotificationAnalyticEvents.PermissionStatus(isAllowed = true))
        viewModelScope.launch {
            neverRequestPermissionUseCase(PUSH_PERMISSION)
        }
    }
}