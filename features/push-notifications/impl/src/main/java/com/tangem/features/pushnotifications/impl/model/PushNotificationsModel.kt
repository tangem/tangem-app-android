package com.tangem.features.pushnotifications.impl.model

import androidx.compose.runtime.Stable
import com.tangem.common.routing.AppRoute
import com.tangem.common.routing.AppRouter
import com.tangem.core.analytics.api.AnalyticsEventHandler
import com.tangem.core.analytics.models.AnalyticsParam
import arrow.core.Either
import com.tangem.core.decompose.di.ModelScoped
import com.tangem.core.decompose.model.Model
import com.tangem.core.decompose.model.ParamsContainer
import com.tangem.domain.account.repository.AccountsCRUDRepository
import com.tangem.domain.common.wallets.UserWalletsListRepository
import com.tangem.domain.notifications.repository.NotificationsRepository
import com.tangem.domain.pushnotificationpreferences.SetAllWalletPushNotificationPreferencesUseCase
import com.tangem.domain.settings.NeverRequestPermissionUseCase
import com.tangem.domain.settings.NeverToInitiallyAskPermissionUseCase
import com.tangem.features.pushnotifications.api.PushNotificationsParams
import com.tangem.features.pushnotifications.api.analytics.PushNotificationAnalyticEvents
import com.tangem.features.pushnotifications.api.utils.PUSH_PERMISSION
import com.tangem.features.pushnotifications.impl.domain.GetPushNotificationsDoubleAskVariantUseCase
import com.tangem.features.pushnotifications.impl.domain.DoubleAskVariant
import com.tangem.features.pushnotificationsettings.PushNotificationSettingsFeatureToggles
import com.tangem.utils.coroutines.CoroutineDispatcherProvider
import com.tangem.utils.coroutines.runSuspendCatching
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
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
    private val notificationsRepository: NotificationsRepository,
    private val pushNotificationSettingsFeatureToggles: PushNotificationSettingsFeatureToggles,
    private val setAllWalletPushNotificationPreferences: SetAllWalletPushNotificationPreferencesUseCase,
    private val userWalletsListRepository: UserWalletsListRepository,
    private val accountsCRUDRepository: AccountsCRUDRepository,
    private val getPushNotificationsDoubleAskVariantUseCase: GetPushNotificationsDoubleAskVariantUseCase,
) : Model(), PushNotificationsClickIntents {

    val params: PushNotificationsParams = paramsContainer.require()

    val isPushNotificationSettingsEnabled: Boolean
        get() = pushNotificationSettingsFeatureToggles.isPushNotificationSettingsEnabled
    val source = when (params.source) {
        AppRoute.PushNotification.Source.Stories -> AnalyticsParam.ScreensSources.Stories
        AppRoute.PushNotification.Source.Main -> AnalyticsParam.ScreensSources.Main
        AppRoute.PushNotification.Source.Onboarding -> AnalyticsParam.ScreensSources.Onboarding
    }

    private val _isDoubleAskSheetShown = MutableStateFlow(false)
    val isDoubleAskSheetShown: StateFlow<Boolean> = _isDoubleAskSheetShown.asStateFlow()

    private var resolvedVariant: String = DoubleAskVariant.Off.key

    init {
        analyticHandler.send(PushNotificationAnalyticEvents.NotificationsScreenOpened(source))
    }

    override fun onAllowClick() {
        modelScope.launch {
            notificationsRepository.setUserAllowToSubscribeOnPushNotifications(true)
        }
        analyticHandler.send(PushNotificationAnalyticEvents.ButtonAllow(source))
    }

    override fun onLaterClick() {
        analyticHandler.send(PushNotificationAnalyticEvents.ButtonLater(source))
        if (isOnWalletScreen()) {
            modelScope.launch { proceedAfterLater() }
            return
        }
        val variant = getPushNotificationsDoubleAskVariantUseCase()
        resolvedVariant = variant.key
        if (variant == DoubleAskVariant.On) {
            analyticHandler.send(PushNotificationAnalyticEvents.WarningScreenShown(source, resolvedVariant))
            _isDoubleAskSheetShown.value = true
        } else {
            modelScope.launch { proceedAfterLater() }
        }
    }

    override fun onDoubleAskEnableClick() {
        analyticHandler.send(PushNotificationAnalyticEvents.WarningScreenEnableTapped(source, resolvedVariant))
        modelScope.launch {
            notificationsRepository.setUserAllowToSubscribeOnPushNotifications(true)
        }
    }

    override fun onDoubleAskSkipClick() {
        analyticHandler.send(PushNotificationAnalyticEvents.WarningScreenSkipTapped(source, resolvedVariant))
        modelScope.launch { proceedAfterLater() }
    }

    override fun onDoubleAskDismiss() {
        _isDoubleAskSheetShown.value = false
    }

    private fun isOnWalletScreen(): Boolean =
        params.isBottomSheet && params.source == AppRoute.PushNotification.Source.Main

    private suspend fun proceedAfterLater() {
        neverRequestPermissionUseCase(PUSH_PERMISSION)
        neverToInitiallyAskPermissionUseCase(PUSH_PERMISSION)
        if (params.isBottomSheet) {
            notificationsRepository.setUserAllowToSubscribeOnPushNotifications(false)
        } else {
            params.nextRoute?.let { appRouter.push(it) }
        }
        params.modelCallbacks.onDenySystemPermission()
    }

    override fun onAllowPermission() {
        analyticHandler.send(
            PushNotificationAnalyticEvents.PermissionStatus(isAllowed = true),
        )
        modelScope.launch {
            neverRequestPermissionUseCase(PUSH_PERMISSION)
            neverToInitiallyAskPermissionUseCase(PUSH_PERMISSION)
            if (isPushNotificationSettingsEnabled) {
                applyFirstActivationRule()
            }
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

    // TODO [REDACTED_JIRA] evaluate per-wallet "first-activation done"
    //  tracking (iOS keeps a [walletId] array in UserDefaults). Today the bulk-enable fires every
    //  time onAllowPermission is called under the feature toggle, but Soft Ask itself is gated by
    //  the existing `shouldShowPushPermission_*` flag so in practice it runs once per install.
    private suspend fun applyFirstActivationRule() {
        userWalletsListRepository.userWalletsSync().forEach { wallet ->
            val result = setAllWalletPushNotificationPreferences(
                userWalletId = wallet.walletId,
                transactionAlerts = true,
                offersUpdates = true,
                priceAlerts = true,
            )
            if (result is Either.Right) {
                runSuspendCatching { accountsCRUDRepository.syncTokens(wallet.walletId) }
            }
        }
    }
}