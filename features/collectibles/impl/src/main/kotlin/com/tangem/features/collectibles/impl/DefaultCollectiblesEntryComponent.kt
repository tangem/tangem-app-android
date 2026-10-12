package com.tangem.features.collectibles.impl

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import com.arkivanov.decompose.extensions.compose.stack.Children
import com.arkivanov.decompose.extensions.compose.stack.animation.slide
import com.arkivanov.decompose.extensions.compose.stack.animation.stackAnimation
import com.arkivanov.decompose.extensions.compose.subscribeAsState
import com.arkivanov.decompose.router.stack.StackNavigation
import com.arkivanov.decompose.router.stack.childStack
import com.arkivanov.decompose.router.stack.pop
import com.tangem.common.routing.entity.CollectiblesStartRoute
import com.tangem.core.decompose.context.AppComponentContext
import com.tangem.core.decompose.context.childByContext
import com.tangem.core.decompose.navigation.inner.InnerRouter
import com.tangem.core.ui.decompose.ComposableContentComponent
import com.tangem.features.collectibles.api.CollectiblesEntryComponent
import com.tangem.features.collectibles.impl.machinedetails.CollectiblesMachineDetailsComponent
import com.tangem.features.collectibles.impl.main.CollectiblesMainComponent
import com.tangem.features.collectibles.impl.onboarding.CollectiblesOnboardingComponent
import com.tangem.features.collectibles.impl.stories.CollectiblesStoriesComponent
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject

@Suppress("LongParameterList")
internal class DefaultCollectiblesEntryComponent @AssistedInject constructor(
    @Assisted context: AppComponentContext,
    @Assisted private val params: CollectiblesEntryComponent.Params,
    private val onboardingComponentFactory: CollectiblesOnboardingComponent.Factory,
    private val storiesComponentFactory: CollectiblesStoriesComponent.Factory,
    private val mainComponentFactory: CollectiblesMainComponent.Factory,
    private val machineDetailsComponentFactory: CollectiblesMachineDetailsComponent.Factory,
) : CollectiblesEntryComponent, AppComponentContext by context {

    private val stackNavigation = StackNavigation<CollectiblesRoute>()

    private val innerRouter = InnerRouter<CollectiblesRoute>(
        stackNavigation = stackNavigation,
        popCallback = { onChildBack() },
    )

    private val stack = childStack(
        key = "collectiblesStack",
        source = stackNavigation,
        serializer = CollectiblesRoute.serializer(),
        initialConfiguration = resolveStartRoute(),
        handleBackButton = true,
        childFactory = { route, factoryContext ->
            createChild(
                route = route,
                childContext = childByContext(componentContext = factoryContext, router = innerRouter),
            )
        },
    )

    private fun resolveStartRoute(): CollectiblesRoute = when (params.startRoute) {
        CollectiblesStartRoute.Onboarding -> CollectiblesRoute.Onboarding(params.userWalletId)
        CollectiblesStartRoute.Main -> CollectiblesRoute.Main(params.userWalletId)
    }

    @Composable
    override fun Content(modifier: Modifier) {
        val childStack by stack.subscribeAsState()

        Children(
            stack = childStack,
            modifier = modifier,
            animation = stackAnimation { slide() },
        ) { child ->
            child.instance.Content(Modifier.fillMaxSize())
        }
    }

    private fun createChild(route: CollectiblesRoute, childContext: AppComponentContext): ComposableContentComponent {
        return when (route) {
            is CollectiblesRoute.Onboarding -> onboardingComponentFactory.create(
                context = childContext,
                params = CollectiblesOnboardingComponent.Params(userWalletId = route.userWalletId),
            )
            CollectiblesRoute.Stories -> storiesComponentFactory.create(context = childContext)
            is CollectiblesRoute.Main -> mainComponentFactory.create(
                context = childContext,
                params = CollectiblesMainComponent.Params(userWalletId = route.userWalletId),
            )
            is CollectiblesRoute.MachineDetails -> machineDetailsComponentFactory.create(
                context = childContext,
                params = CollectiblesMachineDetailsComponent.Params(externalId = route.externalId),
            )
        }
    }

    private fun onChildBack() {
        if (stack.value.backStack.isEmpty()) router.pop() else stackNavigation.pop()
    }

    @AssistedFactory
    interface Factory : CollectiblesEntryComponent.Factory {
        override fun create(
            context: AppComponentContext,
            params: CollectiblesEntryComponent.Params,
        ): DefaultCollectiblesEntryComponent
    }
}