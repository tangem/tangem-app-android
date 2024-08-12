package com.tangem.tap.routing.impl

import com.arkivanov.decompose.ComponentContext
import com.arkivanov.decompose.router.stack.ChildStack
import com.arkivanov.decompose.router.stack.childStack
import com.arkivanov.decompose.value.Value
import com.arkivanov.essenty.backhandler.BackCallback
import com.arkivanov.essenty.lifecycle.doOnDestroy
import com.tangem.common.routing.AppRoute
import com.tangem.core.decompose.context.AppComponentContext
import com.tangem.core.decompose.context.childByContext
import com.tangem.core.decompose.navigation.getOrCreateTyped
import com.tangem.tap.routing.RoutingComponent
import com.tangem.tap.routing.RoutingComponent.Child
import com.tangem.tap.routing.utils.ChildFactory
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject

@Suppress("LongParameterList")
internal class DefaultRoutingComponent @AssistedInject constructor(
    @Assisted context: AppComponentContext,
    private val childFactory: ChildFactory,
) : RoutingComponent, AppComponentContext by context {

    private val backCallback = BackCallback(priority = Int.MIN_VALUE, onBack = router::pop)

    override val stack: Value<ChildStack<AppRoute, Child>> = childStack(
        source = navigationProvider.getOrCreateTyped(),
        serializer = AppRoute.serializer(),
        initialConfiguration = getInitialRoute(),
        handleBackButton = false,
        childFactory = ::child,
    )

    init {
        backHandler.register(backCallback)

        lifecycle.doOnDestroy {
            childFactory.doOnDestroy()
        }
    }

    private fun getInitialRoute(): AppRoute = AppRoute.Initial

    private fun child(route: AppRoute, context: ComponentContext): Child {
        return childFactory.createChild(route) { childByContext(context) }
    }

    @AssistedFactory
    interface Factory : RoutingComponent.Factory {
        override fun create(context: AppComponentContext): DefaultRoutingComponent
    }
}