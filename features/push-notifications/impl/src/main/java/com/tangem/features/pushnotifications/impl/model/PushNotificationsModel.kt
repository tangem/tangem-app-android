package com.tangem.features.pushnotifications.impl.model

import androidx.compose.runtime.Stable
import com.tangem.common.routing.AppRouter
import com.tangem.core.analytics.api.AnalyticsEventHandler
import com.tangem.core.analytics.models.AnalyticsParam
import com.tangem.core.decompose.di.ModelScoped
import com.tangem.core.decompose.model.Model
import com.tangem.core.decompose.model.ParamsContainer
import com.tangem.domain.notifications.toggles.NotificationsFeatureToggles
import com.tangem.domain.settings.NeverRequestPermissionUseCase
import com.tangem.domain.settings.NeverToInitiallyAskPermissionUseCase
import com.tangem.features.pushnotifications.api.PushNotificationsComponent
import com.tangem.features.pushnotifications.api.analytics.PushNotificationAnalyticEvents
import com.tangem.features.pushnotifications.api.utils.PUSH_PERMISSION
import com.tangem.features.pushnotifications.impl.presentation.state.PushNotificationsUM
import com.tangem.utils.coroutines.CoroutineDispatcherProvider
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@Stable
@ModelScoped
@Suppress("LongParameterList")
internal class PushNotificationsModel @Inject constructor(
    paramsContainer: ParamsContainer,
    override val dispatchers: CoroutineDispatcherProvider,
    private val neverRequestPermissionUseCase: NeverRequestPermissionUseCase,
    private val neverToInitiallyAskPermissionUseCase: NeverToInitiallyAskPermissionUseCase,
    private val appRouter: AppRouter,
    private val analyticHandler: AnalyticsEventHandler,
    notificationsFeatureToggles: NotificationsFeatureToggles,
) : Model(), PushNotificationsClickIntents {

    private val params: PushNotificationsComponent.Params = paramsContainer.require()

    private val _state = MutableStateFlow(
        PushNotificationsUM(
            showInfoAboutNotifications = notificationsFeatureToggles.isNotificationsEnabled,
        ),
    )

    val state = _state.asStateFlow()

    override fun onRequest() {
        analyticHandler.send(
            PushNotificationAnalyticEvents.ButtonAllow(AnalyticsParam.ScreensSources.Stories),
        )
    }

    override fun onNeverRequest() {
        analyticHandler.send(
            PushNotificationAnalyticEvents.ButtonLater(AnalyticsParam.ScreensSources.Stories),
        )
        modelScope.launch {
            neverRequestPermissionUseCase(PUSH_PERMISSION)
            neverToInitiallyAskPermissionUseCase(PUSH_PERMISSION)
            onResult()
        }
    }

    override fun onAllowPermission() {
        analyticHandler.send(
            PushNotificationAnalyticEvents.PermissionStatus(isAllowed = true),
        )
        modelScope.launch {
            neverRequestPermissionUseCase(PUSH_PERMISSION)
            neverToInitiallyAskPermissionUseCase(PUSH_PERMISSION)
            onResult()
        }
    }

    override fun onDenyPermission() {
        analyticHandler.send(
            PushNotificationAnalyticEvents.PermissionStatus(isAllowed = false),
        )
        modelScope.launch {
            neverRequestPermissionUseCase(PUSH_PERMISSION)
            neverToInitiallyAskPermissionUseCase(PUSH_PERMISSION)
            onResult()
        }
    }

    private fun onResult() {
        when (params) {
            is PushNotificationsComponent.Params.Callbacks -> {
                params.callbacks.onResult()
            }
            is PushNotificationsComponent.Params.Route -> {
                appRouter.push(params.route)
            }
        }
    }
}