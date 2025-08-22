package com.tangem.features.pushnotifications.impl.model

import androidx.compose.runtime.Stable
import com.tangem.common.routing.AppRoute
import com.tangem.common.routing.AppRouter
import com.tangem.core.analytics.api.AnalyticsEventHandler
import com.tangem.core.analytics.models.AnalyticsParam
import com.tangem.core.decompose.di.ModelScoped
import com.tangem.core.decompose.model.Model
import com.tangem.core.decompose.model.ParamsContainer
import com.tangem.domain.notifications.repository.NotificationsRepository
import com.tangem.domain.notifications.toggles.NotificationsFeatureToggles
import com.tangem.domain.settings.NeverRequestPermissionUseCase
import com.tangem.domain.settings.NeverToInitiallyAskPermissionUseCase
import com.tangem.features.pushnotifications.api.PushNotificationsParams
import com.tangem.features.pushnotifications.api.analytics.PushNotificationAnalyticEvents
import com.tangem.features.pushnotifications.api.utils.PUSH_PERMISSION
import com.tangem.features.pushnotifications.impl.presentation.state.PushNotificationsUM
import com.tangem.utils.coroutines.CoroutineDispatcherProvider
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@Suppress("LongParameterList")
@Stable
@ModelScoped
internal class PushNotificationsModel @Inject constructor(
    paramsContainer: ParamsContainer,
    override val dispatchers: CoroutineDispatcherProvider,
    private val neverRequestPermissionUseCase: NeverRequestPermissionUseCase,
    private val neverToInitiallyAskPermissionUseCase: NeverToInitiallyAskPermissionUseCase,
    private val appRouter: AppRouter,
    private val analyticHandler: AnalyticsEventHandler,
    private val notificationsFeatureToggles: NotificationsFeatureToggles,
    private val notificationsRepository: NotificationsRepository,
) : Model(), PushNotificationsClickIntents {

    val params: PushNotificationsParams = paramsContainer.require()
    val source = when (params.source) {
        AppRoute.PushNotification.Source.Stories -> AnalyticsParam.ScreensSources.Stories
        AppRoute.PushNotification.Source.Main -> AnalyticsParam.ScreensSources.Main
        AppRoute.PushNotification.Source.Onboarding -> AnalyticsParam.ScreensSources.Onboarding
    }

    private val _state = MutableStateFlow(
        PushNotificationsUM(
            showInfoAboutNotifications = notificationsFeatureToggles.isNotificationsEnabled,
        ),
    )

    init {
        analyticHandler.send(PushNotificationAnalyticEvents.NotificationsScreenOpened(source))
    }

    val state = _state.asStateFlow()

    override fun onAllowClick() {
        if (notificationsFeatureToggles.isNotificationsEnabled) {
            modelScope.launch {
                notificationsRepository.setUserAllowToSubscribeOnPushNotifications(true)
            }
        }
        analyticHandler.send(PushNotificationAnalyticEvents.ButtonAllow(source))
    }

    override fun onLaterClick() {
        if (notificationsFeatureToggles.isNotificationsEnabled) {
            modelScope.launch {
                notificationsRepository.setUserAllowToSubscribeOnPushNotifications(false)
            }
        }
        analyticHandler.send(PushNotificationAnalyticEvents.ButtonLater(source))
        modelScope.launch {
            neverRequestPermissionUseCase(PUSH_PERMISSION)
            neverToInitiallyAskPermissionUseCase(PUSH_PERMISSION)
            params.modelCallbacks.onDenySystemPermission()
            if (!params.isBottomSheet) {
                params.nextRoute?.let { appRouter.push(it) }
            }
        }
    }

    override fun onAllowPermission() {
        analyticHandler.send(
            PushNotificationAnalyticEvents.PermissionStatus(isAllowed = true),
        )
        modelScope.launch {
            neverRequestPermissionUseCase(PUSH_PERMISSION)
            neverToInitiallyAskPermissionUseCase(PUSH_PERMISSION)
            params.modelCallbacks.onAllowSystemPermission()
            if (!params.isBottomSheet) {
                params.nextRoute?.let { appRouter.push(it) }
            }
        }
    }

    override fun onDenyPermission() {
        analyticHandler.send(
            PushNotificationAnalyticEvents.PermissionStatus(isAllowed = false),
        )
        modelScope.launch {
            neverRequestPermissionUseCase(PUSH_PERMISSION)
            neverToInitiallyAskPermissionUseCase(PUSH_PERMISSION)
            params.modelCallbacks.onDenySystemPermission()
            if (!params.isBottomSheet) {
                params.nextRoute?.let { appRouter.push(it) }
            }
        }
    }
}