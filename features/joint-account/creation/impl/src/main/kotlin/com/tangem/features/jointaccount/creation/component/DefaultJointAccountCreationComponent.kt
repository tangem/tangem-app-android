package com.tangem.features.jointaccount.creation.component

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import com.arkivanov.decompose.extensions.compose.stack.Children
import com.arkivanov.decompose.extensions.compose.stack.animation.fade
import com.arkivanov.decompose.extensions.compose.stack.animation.stackAnimation
import com.arkivanov.decompose.extensions.compose.subscribeAsState
import com.arkivanov.decompose.router.stack.StackNavigation
import com.arkivanov.decompose.router.stack.childStack
import com.arkivanov.decompose.router.stack.pop
import com.tangem.core.decompose.context.AppComponentContext
import com.tangem.core.decompose.context.childByContext
import com.tangem.core.decompose.navigation.inner.InnerRouter
import com.tangem.core.ui.decompose.ComposableContentComponent
import com.tangem.features.jointaccount.creation.navigation.JointAccountCreationRoute
import com.tangem.features.jointaccount.creation.promo.JointAccountPromoComponent
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject

internal class DefaultJointAccountCreationComponent @AssistedInject constructor(
    @Assisted appComponentContext: AppComponentContext,
    @Assisted private val params: JointAccountCreationComponent.Params,
) : JointAccountCreationComponent, AppComponentContext by appComponentContext {

    private val stackNavigation = StackNavigation<JointAccountCreationRoute>()

    private val innerRouter = InnerRouter<JointAccountCreationRoute>(
        stackNavigation = stackNavigation,
        popCallback = { onChildBack() },
    )

    private val stack = childStack(
        key = "jointAccountCreationStack",
        source = stackNavigation,
        serializer = null,
        initialConfiguration = JointAccountCreationRoute.Promo,
        handleBackButton = true,
        childFactory = { route, factoryContext ->
            createChild(
                route = route,
                childContext = childByContext(componentContext = factoryContext, router = innerRouter),
            )
        },
    )

    @Composable
    override fun Content(modifier: Modifier) {
        val childStack by stack.subscribeAsState()

        Children(
            stack = childStack,
            modifier = modifier,
            animation = stackAnimation { fade() },
        ) { child ->
            child.instance.Content(Modifier.fillMaxSize())
        }
    }

    private fun createChild(
        route: JointAccountCreationRoute,
        childContext: AppComponentContext,
    ): ComposableContentComponent = when (route) {
        is JointAccountCreationRoute.Promo -> JointAccountPromoComponent(
            appComponentContext = childContext,
            params = params,
        )
    }

    private fun onChildBack() {
        if (stack.value.backStack.isEmpty()) {
            router.pop()
        } else {
            stackNavigation.pop()
        }
    }

    @AssistedFactory
    interface Factory : JointAccountCreationComponent.Factory {
        override fun create(
            context: AppComponentContext,
            params: JointAccountCreationComponent.Params,
        ): DefaultJointAccountCreationComponent
    }
}