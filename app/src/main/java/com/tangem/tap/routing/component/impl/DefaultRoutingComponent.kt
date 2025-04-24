package com.tangem.tap.routing.component.impl

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.arkivanov.decompose.ComponentContext
import com.arkivanov.decompose.router.slot.ChildSlot
import com.arkivanov.decompose.router.stack.ChildStack
import com.arkivanov.decompose.router.stack.childStack
import com.arkivanov.decompose.value.MutableValue
import com.arkivanov.decompose.value.Value
import com.arkivanov.decompose.value.subscribe
import com.arkivanov.essenty.lifecycle.doOnDestroy
import com.google.android.material.snackbar.Snackbar
import com.tangem.common.routing.AppRoute
import com.tangem.core.decompose.context.AppComponentContext
import com.tangem.core.decompose.context.childByContext
import com.tangem.core.decompose.navigation.getOrCreateTyped
import com.tangem.core.ui.UiDependencies
import com.tangem.core.ui.extensions.TextReference
import com.tangem.core.ui.extensions.resourceReference
import com.tangem.core.ui.message.SnackbarMessage
import com.tangem.features.walletconnect.components.WcRoutingComponent
import com.tangem.tap.common.SnackbarHandler
import com.tangem.tap.domain.walletconnect2.toggles.WalletConnectFeatureToggles
import com.tangem.tap.routing.RootContent
import com.tangem.tap.routing.component.RoutingComponent
import com.tangem.tap.routing.component.RoutingComponent.Child
import com.tangem.tap.routing.configurator.AppRouterConfig
import com.tangem.tap.routing.toggle.RoutingFeatureToggles
import com.tangem.tap.routing.utils.ChildFactory
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject

@Suppress("LongParameterList")
internal class DefaultRoutingComponent @AssistedInject constructor(
    @Assisted context: AppComponentContext,
    @Assisted val initialStack: List<AppRoute>?,
    private val childFactory: ChildFactory,
    private val appRouterConfig: AppRouterConfig,
    private val uiDependencies: UiDependencies,
    private val wcRoutingComponentFactory: WcRoutingComponent.Factory,
    private val walletConnectFeatureToggles: WalletConnectFeatureToggles,
    routingFeatureToggles: RoutingFeatureToggles,
) : RoutingComponent,
    AppComponentContext by context,
    SnackbarHandler {

    override val stack: Value<ChildStack<AppRoute, Child>> = childStack(
        source = navigationProvider.getOrCreateTyped(),
        initialStack = { getInitialStackOrInit() },
        serializer = null, // AppRoute.serializer(), // Disabled until Nav refactoring completes
        handleBackButton = routingFeatureToggles.isNavigationRefactoringEnabled,
        childFactory = ::child,
    )

    private val wcRoutingComponent: WcRoutingComponent? by lazy {
        if (!walletConnectFeatureToggles.isRedesignedWalletConnectEnabled) return@lazy null
        wcRoutingComponentFactory
            .create(childByContext(componentContext = this), params = Unit)
    }

    init {
        lifecycle.doOnDestroy {
            childFactory.doOnDestroy()
        }

        if (routingFeatureToggles.isNavigationRefactoringEnabled) {
            appRouterConfig.routerScope = componentScope
            appRouterConfig.componentRouter = router
            appRouterConfig.snackbarHandler = this

            stack.subscribe(lifecycle) { stack ->
                val stackItems = stack.items.map { it.configuration }

                if (appRouterConfig.stack != stackItems) {
                    appRouterConfig.stack = stackItems
                }
            }
        }
    }

    @Composable
    override fun Content(modifier: Modifier) {
        RootContent(
            modifier = modifier,
            stack = stack,
            uiDependencies = uiDependencies,
            walletConnectSlot = wcRoutingComponent?.slot ?: MutableValue(ChildSlot(null)),
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

    // TODO: Find correct initial route here: [REDACTED_JIRA]
    private fun getInitialStackOrInit(): List<AppRoute> = if (initialStack.isNullOrEmpty()) {
        listOf(AppRoute.Initial)
    } else {
        initialStack
    }

    private fun child(route: AppRoute, context: ComponentContext): Child {
        val child = childFactory.createChild(route) { childByContext(context) }
        wcRoutingComponent?.onAppRouteChange(route)
        return child
    }

    @AssistedFactory
    interface Factory : RoutingComponent.Factory {
        override fun create(context: AppComponentContext, initialStack: List<AppRoute>?): DefaultRoutingComponent
    }
}