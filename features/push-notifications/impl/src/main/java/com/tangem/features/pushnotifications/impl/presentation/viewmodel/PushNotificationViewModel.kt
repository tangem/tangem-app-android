package com.tangem.features.pushnotifications.impl.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.tangem.core.analytics.api.AnalyticsEventHandler
import com.tangem.core.analytics.models.AnalyticsParam
import com.tangem.core.navigation.settings.SettingsManager
import com.tangem.domain.settings.DelayPermissionRequestUseCase
import com.tangem.domain.settings.NeverRequestPermissionUseCase
import com.tangem.domain.settings.NeverToInitiallyAskPermissionUseCase
import com.tangem.domain.settings.SetFirstTimeAskingPermissionUseCase
import com.tangem.features.pushnotifications.api.analytics.PushNotificationAnalyticEvents
import com.tangem.features.pushnotifications.api.utils.PUSH_PERMISSION
import com.tangem.features.pushnotifications.impl.navigation.DefaultPushNotificationsRouter
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

@Suppress("LongParameterList")
@HiltViewModel
internal class PushNotificationViewModel @Inject constructor(
    private val setFirstTimeAskingPermissionUseCase: SetFirstTimeAskingPermissionUseCase,
    private val delayPermissionRequestUseCase: DelayPermissionRequestUseCase,
    private val neverRequestPermissionUseCase: NeverRequestPermissionUseCase,
    private val neverToInitiallyAskPermissionUseCase: NeverToInitiallyAskPermissionUseCase,
    private val router: DefaultPushNotificationsRouter,
    private val settingsManager: SettingsManager,
    private val analyticHandler: AnalyticsEventHandler,
) : ViewModel(), PushNotificationsClickIntents {

    override fun onRequest() {
        analyticHandler.send(
            PushNotificationAnalyticEvents.ButtonAllow(AnalyticsParam.ScreensSources.Stories),
        )
        viewModelScope.launch {
            setFirstTimeAskingPermissionUseCase(PUSH_PERMISSION)
        }
    }

    override fun onRequestLater() {
        analyticHandler.send(
            PushNotificationAnalyticEvents.ButtonLater(AnalyticsParam.ScreensSources.Stories),
        )
        viewModelScope.launch {
            delayPermissionRequestUseCase(PUSH_PERMISSION)
            setFirstTimeAskingPermissionUseCase(PUSH_PERMISSION)
            neverToInitiallyAskPermissionUseCase(PUSH_PERMISSION)
            router.openHome()
        }
    }

    override fun onAllowPermission() {
        analyticHandler.send(
            PushNotificationAnalyticEvents.PermissionStatus(isAllowed = true),
        )
        viewModelScope.launch {
            neverRequestPermissionUseCase(PUSH_PERMISSION)
            neverToInitiallyAskPermissionUseCase(PUSH_PERMISSION)
            router.openHome()
        }
    }

    override fun onDenyPermission() {
        analyticHandler.send(
            PushNotificationAnalyticEvents.PermissionStatus(isAllowed = false),
        )
        viewModelScope.launch {
            delayPermissionRequestUseCase(PUSH_PERMISSION)
            neverToInitiallyAskPermissionUseCase(PUSH_PERMISSION)
            router.openHome()
        }
    }

    override fun openSettings() = settingsManager.openSettings()
}
