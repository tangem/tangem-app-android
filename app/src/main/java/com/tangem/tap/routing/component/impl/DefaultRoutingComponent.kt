package com.tangem.tap.routing.component.impl

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.arkivanov.decompose.router.stack.ChildStack
import com.arkivanov.decompose.router.stack.childStack
import com.arkivanov.decompose.router.stack.pop
import com.arkivanov.decompose.value.Value
import com.arkivanov.decompose.value.subscribe
import com.arkivanov.essenty.lifecycle.subscribe
import com.google.android.material.snackbar.Snackbar
import com.tangem.common.routing.AppRoute
import com.tangem.common.routing.entity.InitScreenLaunchMode
import com.tangem.common.ui.notifications.NotificationId
import com.tangem.core.analytics.api.AnalyticsEventHandler
import com.tangem.core.analytics.api.AnalyticsExceptionHandler
import com.tangem.core.analytics.models.Basic
import com.tangem.core.analytics.models.ExceptionAnalyticsEvent
import com.tangem.core.analytics.utils.TrackingContextProxy
import com.tangem.core.decompose.context.AppComponentContext
import com.tangem.core.decompose.context.child
import com.tangem.core.decompose.context.childByContext
import com.tangem.core.decompose.navigation.getOrCreateTyped
import com.tangem.core.ui.UiDependencies
import com.tangem.core.ui.extensions.TextReference
import com.tangem.core.ui.extensions.resourceReference
import com.tangem.core.ui.message.DialogMessage
import com.tangem.core.ui.message.EventMessageAction
import com.tangem.core.ui.message.SnackbarMessage
import com.tangem.domain.card.repository.CardRepository
import com.tangem.domain.common.wallets.UserWalletsListRepository
import com.tangem.domain.models.scan.ScanResponse
import com.tangem.domain.models.wallet.UserWallet
import com.tangem.domain.models.wallet.isImported
import com.tangem.domain.models.wallet.isLocked
import com.tangem.domain.notifications.repository.NotificationsRepository
import com.tangem.domain.onboarding.repository.OnboardingRepository
import com.tangem.domain.settings.NeverRequestPermissionUseCase
import com.tangem.domain.settings.NeverToInitiallyAskPermissionUseCase
import com.tangem.domain.settings.ShouldInitiallyAskPermissionUseCase
import com.tangem.features.hotwallet.HotAccessCodeRequestComponent
import com.tangem.features.hotwallet.accesscoderequest.proxy.HotWalletPasswordRequesterProxy
import com.tangem.features.onboarding.v2.common.analytics.OnboardingEvent
import com.tangem.features.pushnotifications.api.utils.PUSH_PERMISSION
import com.tangem.features.walletconnect.components.WcRoutingComponent
import com.tangem.hot.sdk.TangemHotSdk
import com.tangem.hot.sdk.android.create
import com.tangem.sdk.api.BackupServiceHolder
import com.tangem.tap.common.SnackbarHandler
import com.tangem.tap.common.analytics.events.Onboarding
import com.tangem.tap.features.demo.DemoHelper
import com.tangem.tap.features.hot.TangemHotSDKProxy
import com.tangem.tap.features.root.RootDetectedWarningComponent
import com.tangem.tap.features.scanfails.ScanFailsComponent
import com.tangem.tap.features.scanfails.ScanFailsRequesterProxy
import com.tangem.tap.routing.RootContent
import com.tangem.tap.routing.component.RoutingComponent
import com.tangem.tap.routing.component.RoutingComponent.Child
import com.tangem.tap.routing.configurator.AppRouterConfig
import com.tangem.tap.routing.utils.ChildFactory
import com.tangem.tap.routing.utils.DeepLinkFactory
import com.tangem.tap.store
import com.tangem.utils.logging.TangemLogger
import com.tangem.wallet.R
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import kotlinx.coroutines.launch

@Suppress("LongParameterList")
internal class DefaultRoutingComponent @AssistedInject constructor(
    @Assisted context: AppComponentContext,
    @Assisted val initialStack: List<AppRoute>?,
    @Assisted val launchMode: InitScreenLaunchMode,
    private val childFactory: ChildFactory,
    private val appRouterConfig: AppRouterConfig,
    private val uiDependencies: UiDependencies,
    private val wcRoutingComponentFactory: WcRoutingComponent.Factory,
    private val deeplinkFactory: DeepLinkFactory,
    private val tangemHotSDKProxy: TangemHotSDKProxy,
    private val hotAccessCodeRequestComponentFactory: HotAccessCodeRequestComponent.Factory,
    private val hotAccessCodeRequesterProxy: HotWalletPasswordRequesterProxy,
    private val rootDetectedWarningComponentFactory: RootDetectedWarningComponent.Factory,
    private val userWalletsListRepository: UserWalletsListRepository,
    private val cardRepository: CardRepository,
    private val onboardingRepository: OnboardingRepository,
    private val trackingContextProxy: TrackingContextProxy,
    private val scanFailsComponentFactory: ScanFailsComponent.Factory,
    private val scanFailsRequesterProxy: ScanFailsRequesterProxy,
    private val analyticsEventHandler: AnalyticsEventHandler,
    private val analyticsExceptionHandler: AnalyticsExceptionHandler,
    private val backupServiceHolder: BackupServiceHolder,
    private val notificationsRepository: NotificationsRepository,
    private val neverToInitiallyAskPermissionUseCase: NeverToInitiallyAskPermissionUseCase,
    private val shouldInitiallyAskPermissionUseCase: ShouldInitiallyAskPermissionUseCase,
    private val neverRequestPermissionUseCase: NeverRequestPermissionUseCase,
) : RoutingComponent,
    AppComponentContext by context,
    SnackbarHandler {

    private val wcRoutingComponent: WcRoutingComponent by lazy {
        wcRoutingComponentFactory
            .create(child("wcRoutingComponent"), params = Unit)
    }

    private val hotAccessCodeRequestComponent: HotAccessCodeRequestComponent by lazy {
        hotAccessCodeRequestComponentFactory
            .create(child("hotAccessCodeRequestComponent"), Unit)
    }

    private val rootDetectedWarningComponent: RootDetectedWarningComponent by lazy {
        rootDetectedWarningComponentFactory
            .create(child("rootDetectedWarningComponent"), Unit)
    }

    private val scanFailsComponent: ScanFailsComponent by lazy {
        scanFailsComponentFactory
            .create(child("scanFailsComponent"), Unit)
    }

    private val navigation = navigationProvider.getOrCreateTyped<AppRoute>()

    private val stack: Value<ChildStack<AppRoute, Child>> = childStack(
        source = navigation,
        initialStack = { getInitialStackOrInit() },
        serializer = null, // AppRoute.serializer(), // Disabled until Nav refactoring completes
        handleBackButton = true,
        childFactory = { route, childContext ->
            try {
                childFactory.createChild(route, childByContext(childContext))
            } catch (e: Exception) {
                TangemLogger.e("App Router Failed", e)
                analyticsExceptionHandler.sendException(
                    ExceptionAnalyticsEvent(exception = e, params = mapOf("Category" to "App Routing")),
                )
                Child.DummyComponent
            }
        },
    )

    init {
        appRouterConfig.routerScope = componentScope
        appRouterConfig.componentRouter = router
        appRouterConfig.snackbarHandler = this

        stack.subscribe(lifecycle) { stack ->
            // Handle DummyComponent by popping it immediately
            if (stack.active.instance is Child.DummyComponent) {
                navigation.pop()
                return@subscribe
            }

            val stackItems = stack.items.map { it.configuration }

            wcRoutingComponent.onAppRouteChange(stack.active.configuration)
            deeplinkFactory.checkRoutingReadiness(stack.active.configuration)

            if (appRouterConfig.stack != stackItems) {
                appRouterConfig.stack = stackItems
            }
        }

        configureProxies()
        initializeInitialNavigation()
    }

    private fun initializeInitialNavigation() {
        if (initialStack.isNullOrEmpty()) {
            componentScope.launch {
                val initialRoute = resolveInitialRoute()
                if (rootDetectedWarningComponent.shouldShowWarning()) {
                    launch(dispatchers.main) {
                        rootDetectedWarningComponent.tryToShowWarningAndWaitContinuation()
                        router.replaceAll(initialRoute)
                    }
                } else {
                    router.replaceAll(initialRoute)
                }
            }
        }
    }

    private suspend fun resolveInitialRoute(): AppRoute {
        val userWallets = userWalletsListRepository.userWalletsSync()

        return when {
            userWallets.isEmpty() -> navigateForEmptyWallets()
            userWallets.any { it.isLocked } -> {
                AppRoute.Welcome(
                    launchMode = launchMode,
                )
            }
            else -> {
                trackSignInEvent()
                AppRoute.Wallet
            }
        }.also {
            appRouterConfig.initializedState.value = true
            checkForUnfinishedBackup()
        }
    }

    private suspend fun navigateForEmptyWallets(): AppRoute {
        val shouldAskPushPermission = shouldInitiallyAskPermissionUseCase(PUSH_PERMISSION).getOrNull()
            ?: return AppRoute.Home(launchMode = launchMode)
        return if (shouldAskPushPermission) {
            notificationsRepository.setShouldShowNotifications(
                key = NotificationId.EnablePushesReminderNotification.key,
                value = false,
            )
            AppRoute.PushNotification(AppRoute.PushNotification.Source.Stories)
        } else {
            neverToInitiallyAskPermissionUseCase(PUSH_PERMISSION)
            neverRequestPermissionUseCase(PUSH_PERMISSION)
            AppRoute.Home(launchMode = launchMode)
        }
    }

    @Composable
    override fun Content(modifier: Modifier) {
        RootContent(
            stack = stack,
            backHandler = backHandler,
            uiDependencies = uiDependencies,
            onBack = router::pop,
            modifier = modifier,
            wcContent = { wcRoutingComponent.Content(it) },
            hotAccessCodeContent = { hotAccessCodeRequestComponent.Content(it) },
            rootDetectedWarningContent = { rootDetectedWarningComponent.Content(it) },
            scanFailsContent = { scanFailsComponent.Content(it) },
        )
    }

    override fun showSnackbar(text: Int, length: Int, buttonTitle: Int?, action: (() -> Unit)?) {
        showSnackbar(
            text = resourceReference(text),
            length = length,
            buttonTitle = buttonTitle?.let(::resourceReference),
            action = action,
        )
    }

    override fun showSnackbar(text: TextReference, length: Int, buttonTitle: TextReference?, action: (() -> Unit)?) {
        val message = SnackbarMessage(
            message = text,
            duration = when (length) {
                Snackbar.LENGTH_SHORT -> SnackbarMessage.Duration.Short
                Snackbar.LENGTH_LONG -> SnackbarMessage.Duration.Long
                Snackbar.LENGTH_INDEFINITE -> SnackbarMessage.Duration.Indefinite
                else -> SnackbarMessage.Duration.Short
            },
            actionLabel = buttonTitle,
            action = action,
        )

        messageSender.send(message)
    }

    override fun dismissSnackbar() {
        messageSender.send(SnackbarMessage(message = TextReference.EMPTY))
    }

    private fun getInitialStackOrInit(): List<AppRoute> = if (initialStack.isNullOrEmpty()) {
        listOf(AppRoute.Initial)
    } else {
        initialStack
    }

    private fun configureProxies() {
        lifecycle.subscribe(
            onCreate = {
                tangemHotSDKProxy.sdkState.value = TangemHotSdk.create(activity)
                hotAccessCodeRequesterProxy.componentRequester.value = hotAccessCodeRequestComponent
                scanFailsRequesterProxy.componentRequester.value = scanFailsComponent
            },
            onDestroy = {
                tangemHotSDKProxy.sdkState.value = null
                hotAccessCodeRequesterProxy.componentRequester.value = null
                scanFailsRequesterProxy.componentRequester.value = null
            },
        )
    }

    @AssistedFactory
    interface Factory : RoutingComponent.Factory {
        override fun create(
            context: AppComponentContext,
            initialStack: List<AppRoute>?,
            launchMode: InitScreenLaunchMode,
        ): DefaultRoutingComponent
    }

    private fun checkForUnfinishedBackup() {
        if (DemoHelper.tryHandle { store.state }) return
        componentScope.launch(dispatchers.main) {
            val scanResponse = onboardingRepository.getUnfinishedFinalizeOnboarding() ?: return@launch
            messageSender.send(unfinishedBackupFoundDialog(scanResponse))
        }
    }

    private fun unfinishedBackupFoundDialog(scanResponse: ScanResponse): DialogMessage = DialogMessage(
        title = resourceReference(R.string.common_warning),
        message = resourceReference(R.string.welcome_interrupted_backup_alert_message),
        isDismissable = false,
        firstActionBuilder = {
            EventMessageAction(
                title = resourceReference(R.string.welcome_interrupted_backup_alert_resume),
                onClick = {
                    analyticsEventHandler.send(OnboardingEvent.Backup.ResumeInterruptedBackup())
                    resumeUnfinishedBackup(scanResponse)
                },
            )
        },
        secondActionBuilder = {
            EventMessageAction(
                title = resourceReference(R.string.welcome_interrupted_backup_alert_discard),
                onClick = {
                    analyticsEventHandler.send(OnboardingEvent.Backup.CancelInterruptedBackup())
                    messageSender.send(confirmDiscardingBackupDialog(scanResponse))
                },
            )
        },
    )

    private fun confirmDiscardingBackupDialog(scanResponse: ScanResponse): DialogMessage = DialogMessage(
        title = resourceReference(R.string.welcome_interrupted_backup_discard_title),
        message = resourceReference(R.string.welcome_interrupted_backup_discard_message),
        isDismissable = false,
        firstActionBuilder = {
            EventMessageAction(
                title = resourceReference(R.string.welcome_interrupted_backup_discard_resume),
                onClick = { resumeUnfinishedBackup(scanResponse) },
            )
        },
        secondActionBuilder = {
            EventMessageAction(
                title = resourceReference(R.string.welcome_interrupted_backup_discard_discard),
                onClick = { discardSavedBackup() },
            )
        },
    )

    private fun resumeUnfinishedBackup(scanResponse: ScanResponse) {
        router.replaceAll(
            AppRoute.Onboarding(
                scanResponse = scanResponse,
                mode = AppRoute.Onboarding.Mode.ContinueFinalize,
            ),
        )
    }

    private fun discardSavedBackup() {
        componentScope.launch(dispatchers.main) {
            backupServiceHolder.backupService.get()?.discardSavedBackup()
            val unfinishedBackup = onboardingRepository.getUnfinishedFinalizeOnboarding() ?: return@launch
            cardRepository.finishCardActivation(unfinishedBackup.card.cardId)
            onboardingRepository.clearUnfinishedFinalizeOnboarding()
            analyticsEventHandler.send(Onboarding.Finished())
        }
    }

    private suspend fun trackSignInEvent() {
        val userWallets = userWalletsListRepository.userWalletsSync()
        val selectedWallet = userWalletsListRepository.selectedUserWalletSync() ?: return
        trackingContextProxy.addContext(selectedWallet)
        val isBackedUp = when (selectedWallet) {
            is UserWallet.Cold -> selectedWallet.scanResponse.card.backupStatus?.isActive == true
            is UserWallet.Hot -> selectedWallet.backedUp
        }
        analyticsEventHandler.send(
            event = Basic.SignedIn(
                signInType = Basic.SignedIn.SignInType.NoSecurity,
                walletsCount = userWallets.size,
                isImported = selectedWallet.isImported(),
                hasBackup = isBackedUp,
            ),
        )
    }
}