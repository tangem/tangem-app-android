package com.tangem.tap.routing.component.impl

import androidx.compose.material3.SnackbarDuration
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.arkivanov.decompose.ComponentContext
import com.arkivanov.decompose.router.stack.ChildStack
import com.arkivanov.decompose.router.stack.childStack
import com.arkivanov.decompose.value.Value
import com.arkivanov.decompose.value.observe
import com.arkivanov.essenty.backhandler.BackCallback
import com.arkivanov.essenty.lifecycle.doOnDestroy
import com.google.android.material.snackbar.Snackbar
import com.tangem.common.routing.AppRoute
import com.tangem.core.decompose.context.AppComponentContext
import com.tangem.core.decompose.context.childByContext
import com.tangem.core.decompose.navigation.getOrCreateTyped
import com.tangem.core.ui.extensions.TextReference
import com.tangem.core.ui.extensions.resourceReference
import com.tangem.core.ui.message.SnackbarMessage
import com.tangem.tap.common.SnackbarHandler
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
    private val childFactory: ChildFactory,
    private val appRouterConfig: AppRouterConfig,
    private val routingFeatureToggles: RoutingFeatureToggles,
) : RoutingComponent,
    AppComponentContext by context,
    SnackbarHandler {

    private val backCallback = BackCallback(priority = Int.MIN_VALUE, onBack = router::pop)

    override val stack: Value<ChildStack<AppRoute, Child>> = childStack(
        source = navigationProvider.getOrCreateTyped(),
        serializer = AppRoute.serializer(), // TODO Maybe set this to null for AppRoute.Onboarding case. Need to check
        initialConfiguration = getInitialRoute(),
        handleBackButton = false,
        childFactory = ::child,
    )

    init {
        backHandler.register(backCallback)

        lifecycle.doOnDestroy {
            childFactory.doOnDestroy()
        }

        if (routingFeatureToggles.isNavigationRefactoringEnabled) {
            appRouterConfig.routerScope = componentScope
            appRouterConfig.componentRouter = router
            appRouterConfig.snackbarHandler = this

            stack.observe(lifecycle) { stack ->
                val stackItems = stack.items.map { it.configuration }

                if (appRouterConfig.stack != stackItems) {
                    appRouterConfig.stack = stackItems
                }
            }
        }
    }

    @Composable
    override fun Content(modifier: Modifier) {
        // TODO: Implement in [REDACTED_JIRA]
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
                Snackbar.LENGTH_SHORT -> SnackbarDuration.Short
                Snackbar.LENGTH_LONG -> SnackbarDuration.Long
                Snackbar.LENGTH_INDEFINITE -> SnackbarDuration.Indefinite
                else -> SnackbarDuration.Short
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
    private fun getInitialRoute(): AppRoute = AppRoute.Initial

    private fun child(route: AppRoute, context: ComponentContext): Child {
        return childFactory.createChild(route) { childByContext(context) }
    }

    @AssistedFactory
    interface Factory : RoutingComponent.Factory {
        override fun create(context: AppComponentContext): DefaultRoutingComponent
    }
}