package com.tangem.feature.wallet.presentation.wallet.viewmodels.intents

import com.tangem.core.analytics.api.AnalyticsEventHandler
import com.tangem.core.analytics.models.AnalyticsParam
import com.tangem.domain.settings.DelayPermissionRequestUseCase
import com.tangem.domain.settings.NeverRequestPermissionUseCase
import com.tangem.domain.settings.SetFirstTimeAskingPermissionUseCase
import com.tangem.features.pushnotifications.api.analytics.PushNotificationAnalyticEvents
import com.tangem.features.pushnotifications.api.utils.PUSH_PERMISSION
import dagger.hilt.android.scopes.ViewModelScoped
import kotlinx.coroutines.launch
import javax.inject.Inject

internal interface WalletPushPermissionClickIntents {

    fun onRequestPushPermission()

    fun onDelayAskPushPermission()

    fun onNeverAskPushPermission()

    fun onDenyPushPermission()

    fun onAllowPushPermission()
}

@ViewModelScoped
internal class WalletPushPermissionClickIntentsImplementor @Inject constructor(
    private val setFirstTimeAskingPermissionUseCase: SetFirstTimeAskingPermissionUseCase,
    private val neverRequestPermissionUseCase: NeverRequestPermissionUseCase,
    private val delayPermissionRequestUseCase: DelayPermissionRequestUseCase,
    private val analyticsEventHandler: AnalyticsEventHandler,
) : BaseWalletClickIntents(), WalletPushPermissionClickIntents {

    override fun onRequestPushPermission() {
        analyticsEventHandler.send(
            PushNotificationAnalyticEvents.ButtonAllow(AnalyticsParam.ScreensSources.Main),
        )
        viewModelScope.launch {
            setFirstTimeAskingPermissionUseCase(PUSH_PERMISSION)
        }
    }

    override fun onDelayAskPushPermission() {
        viewModelScope.launch {
            analyticsEventHandler.send(
                PushNotificationAnalyticEvents.ButtonLater(AnalyticsParam.ScreensSources.Main),
            )
            delayPermissionRequestUseCase(PUSH_PERMISSION)
        }
    }

    override fun onNeverAskPushPermission() {
        viewModelScope.launch {
            analyticsEventHandler.send(PushNotificationAnalyticEvents.ButtonCancel)
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
