package com.tangem.features.pushnotifications.impl.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.tangem.core.analytics.api.AnalyticsEventHandler
import com.tangem.core.analytics.models.AnalyticsParam
import com.tangem.domain.settings.NeverRequestPermissionUseCase
import com.tangem.features.pushnotifications.api.analytics.PushNotificationAnalyticEvents
import com.tangem.features.pushnotifications.api.utils.PUSH_PERMISSION
import com.tangem.features.pushnotifications.impl.navigation.DefaultPushNotificationsRouter
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

@Suppress("LongParameterList")
@HiltViewModel
internal class PushNotificationViewModel @Inject constructor(
    private val neverRequestPermissionUseCase: NeverRequestPermissionUseCase,
    private val router: DefaultPushNotificationsRouter,
    private val analyticHandler: AnalyticsEventHandler,
) : ViewModel(), PushNotificationsClickIntents {

    override fun onRequest() {
        analyticHandler.send(
            PushNotificationAnalyticEvents.ButtonAllow(AnalyticsParam.ScreensSources.Stories),
        )
    }

    override fun onNeverRequest() {
        analyticHandler.send(
            PushNotificationAnalyticEvents.ButtonCancel(AnalyticsParam.ScreensSources.Stories),
        )
        viewModelScope.launch {
            neverRequestPermissionUseCase(PUSH_PERMISSION)
            router.openHome()
        }
    }

    override fun onAllowPermission() {
        analyticHandler.send(
            PushNotificationAnalyticEvents.PermissionStatus(isAllowed = true),
        )
        viewModelScope.launch {
            neverRequestPermissionUseCase(PUSH_PERMISSION)
            router.openHome()
        }
    }

    override fun onDenyPermission() {
        analyticHandler.send(
            PushNotificationAnalyticEvents.PermissionStatus(isAllowed = false),
        )
        viewModelScope.launch {
            neverRequestPermissionUseCase(PUSH_PERMISSION)
            router.openHome()
        }
    }
}
