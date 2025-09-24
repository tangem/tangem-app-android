package com.tangem.tap.routing.component.impl

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.arkivanov.decompose.router.stack.ChildStack
import com.arkivanov.decompose.router.stack.childStack
import com.arkivanov.decompose.value.Value
import com.arkivanov.decompose.value.subscribe
import com.arkivanov.essenty.lifecycle.subscribe
import com.google.android.material.snackbar.Snackbar
import com.tangem.common.routing.AppRoute
import com.tangem.common.routing.entity.InitScreenLaunchMode
import com.tangem.core.decompose.context.AppComponentContext
import com.tangem.core.decompose.context.child
import com.tangem.core.decompose.context.childByContext
import com.tangem.core.decompose.navigation.getOrCreateTyped
import com.tangem.core.ui.UiDependencies
import com.tangem.core.ui.extensions.TextReference
import com.tangem.core.ui.extensions.resourceReference
import com.tangem.core.ui.message.SnackbarMessage
import com.tangem.domain.card.repository.CardRepository
import com.tangem.domain.core.wallets.UserWalletsListRepository
import com.tangem.domain.models.wallet.isLocked
import com.tangem.domain.onboarding.repository.OnboardingRepository
import com.tangem.features.hotwallet.HotAccessCodeRequestComponent
import com.tangem.features.hotwallet.accesscoderequest.proxy.HotWalletPasswordRequesterProxy
import com.tangem.features.walletconnect.components.WcRoutingComponent
import com.tangem.hot.sdk.TangemHotSdk
import com.tangem.hot.sdk.android.create
import com.tangem.tap.common.SnackbarHandler
import com.tangem.tap.common.redux.global.GlobalAction
import com.tangem.tap.features.hot.TangemHotSDKProxy
import com.tangem.tap.features.onboarding.products.wallet.redux.BackupDialog
import com.tangem.tap.routing.RootContent
import com.tangem.tap.routing.component.RoutingComponent
import com.tangem.tap.routing.component.RoutingComponent.Child
import com.tangem.tap.routing.configurator.AppRouterConfig
import com.tangem.tap.routing.utils.ChildFactory
import com.tangem.tap.routing.utils.DeepLinkFactory
import com.tangem.tap.store
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
    private val userWalletsListRepository: UserWalletsListRepository,
    private val cardRepository: CardRepository,
    private val onboardingRepository: OnboardingRepository,
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

    private val stack: Value<ChildStack<AppRoute, Child>> = childStack(
        source = navigationProvider.getOrCreateTyped(),
        initialStack = { getInitialStackOrInit() },
        serializer = null, // AppRoute.serializer(), // Disabled until Nav refactoring completes
        handleBackButton = true,
        childFactory = { route, childContext ->
            childFactory.createChild(route, childByContext(childContext))
        },
    )

    init {
        appRouterConfig.routerScope = componentScope
        appRouterConfig.componentRouter = router
        appRouterConfig.snackbarHandler = this

        stack.subscribe(lifecycle) { stack ->
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
                router.replaceAll(initialRoute)
            }
        }
    }

    private suspend fun resolveInitialRoute(): AppRoute {
        val userWallets = userWalletsListRepository.userWalletsSync()

        return when {
            userWallets.isEmpty() -> {
                val shouldShowTos = !cardRepository.isTangemTOSAccepted()
                if (shouldShowTos) {
                    AppRoute.Disclaimer(isTosAccepted = false)
                } else {
                    AppRoute.Home(launchMode = launchMode)
                }
            }
            userWallets.any { it.isLocked } -> {
                AppRoute.Welcome(
                    launchMode = launchMode,
                )
            }
            else -> {
                AppRoute.Wallet
            }
        }.also {
            appRouterConfig.initializedState.value = true
            checkForUnfinishedBackup()
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
            },
            onDestroy = {
                tangemHotSDKProxy.sdkState.value = null
                hotAccessCodeRequesterProxy.componentRequester.value = null
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
        componentScope.launch(dispatchers.main) {
            val onboardingScanResponse = onboardingRepository.getUnfinishedFinalizeOnboarding() ?: return@launch
            store.dispatch(GlobalAction.ShowDialog(BackupDialog.UnfinishedBackupFound(onboardingScanResponse)))
        }
    }
}