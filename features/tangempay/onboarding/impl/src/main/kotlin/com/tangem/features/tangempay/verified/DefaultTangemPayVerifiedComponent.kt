package com.tangem.features.tangempay.verified

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.arkivanov.decompose.ComponentContext
import com.arkivanov.decompose.extensions.compose.stack.Children
import com.arkivanov.decompose.extensions.compose.stack.animation.stackAnimation
import com.arkivanov.decompose.extensions.compose.subscribeAsState
import com.arkivanov.decompose.router.stack.childStack
import com.tangem.core.decompose.context.AppComponentContext
import com.tangem.core.decompose.context.childByContext
import com.tangem.core.decompose.model.getOrCreateModel
import com.tangem.core.decompose.navigation.inner.InnerRouter
import com.tangem.core.ui.decompose.ComposableContentComponent
import com.tangem.core.ui.res.TangemThemeRedesign
import com.tangem.features.tangempay.components.TangemPaySelectPlanComponent
import com.tangem.features.tangempay.components.TangemPayVerifiedComponent
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject

internal class DefaultTangemPayVerifiedComponent @AssistedInject constructor(
    @Assisted private val context: AppComponentContext,
    @Assisted private val params: TangemPayVerifiedComponent.Params,
    private val selectPlanComponentFactory: TangemPaySelectPlanComponent.Factory,
) : TangemPayVerifiedComponent, AppComponentContext by context {

    private val model: TangemPayVerifiedModel = getOrCreateModel(params)

    private val innerRouter = InnerRouter(stackNavigation = model.stackNavigation)

    private val verifiedContent = ComposableContentComponent { modifier ->
        val state by model.uiState.collectAsStateWithLifecycle()
        TangemThemeRedesign {
            TangemPayVerifiedScreen(
                state = state,
                animated = true,
                modifier = modifier,
            )
        }
    }

    private val childStack = childStack(
        key = "tangemPayVerifiedStack",
        source = model.stackNavigation,
        serializer = TangemPayVerifiedRoute.serializer(),
        initialConfiguration = TangemPayVerifiedRoute.Verified,
        childFactory = ::child,
    )

    @Composable
    override fun Content(modifier: Modifier) {
        val childStack by childStack.subscribeAsState()
        Children(stack = childStack, modifier = modifier, animation = stackAnimation()) { child ->
            child.instance.Content(modifier = Modifier.fillMaxSize())
        }
    }

    private fun child(route: TangemPayVerifiedRoute, componentContext: ComponentContext): ComposableContentComponent {
        return when (route) {
            TangemPayVerifiedRoute.Verified -> verifiedContent
            is TangemPayVerifiedRoute.SelectPlan -> selectPlanComponentFactory.create(
                context = childByContext(componentContext = componentContext, router = innerRouter),
                params = TangemPaySelectPlanComponent.Params(
                    userWalletId = params.userWalletId,
                    mode = TangemPaySelectPlanComponent.Mode.CreateOnboardingOrder(
                        availableProducts = route.availableProducts,
                    ),
                ),
            )
        }
    }

    @AssistedFactory
    interface Factory : TangemPayVerifiedComponent.Factory {
        override fun create(
            context: AppComponentContext,
            params: TangemPayVerifiedComponent.Params,
        ): DefaultTangemPayVerifiedComponent
    }
}