package com.tangem.features.tangempay.eligibility

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import com.arkivanov.decompose.ComponentContext
import com.arkivanov.decompose.extensions.compose.stack.Children
import com.arkivanov.decompose.extensions.compose.subscribeAsState
import com.arkivanov.decompose.router.stack.StackNavigation
import com.arkivanov.decompose.router.stack.childStack
import com.arkivanov.decompose.router.stack.pop
import com.tangem.core.analytics.api.AnalyticsEventHandler
import com.tangem.core.decompose.context.AppComponentContext
import com.tangem.core.decompose.context.childByContext
import com.tangem.core.decompose.navigation.inner.InnerRouter
import com.tangem.core.ui.decompose.ComposableContentComponent
import com.tangem.features.tangempay.eligibility.country.ResidencyListComponent
import com.tangem.features.tangempay.eligibility.unavailable.RegionUnavailableComponent
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject

internal class DefaultTangemPayEligibilityComponent @AssistedInject constructor(
    @Assisted context: AppComponentContext,
    @Assisted private val params: TangemPayEligibilityComponent.Params,
    private val waitlistComponentFactory: TangemPayWaitlistComponent.Factory,
    private val walletSelectorComponentFactory: TangemPayWalletSelectorComponent.Factory,
    private val analytics: AnalyticsEventHandler,
) : TangemPayEligibilityComponent, AppComponentContext by context {

    private val stackNavigation = StackNavigation<EligibilityInnerRoute>()

    private val innerRouter = InnerRouter(
        stackNavigation = stackNavigation,
        popCallback = { onChildBack() },
    )

    private val childStack = childStack(
        key = "tangemPayEligibilityInnerStack",
        source = stackNavigation,
        serializer = EligibilityInnerRoute.serializer(),
        initialConfiguration = EligibilityInnerRoute.Residency,
        childFactory = ::screenChild,
    )

    @Composable
    override fun Content(modifier: Modifier) {
        val stack by childStack.subscribeAsState()

        BackHandler(onBack = ::onChildBack)
        Children(modifier = modifier, stack = stack) { child ->
            child.instance.Content(modifier = Modifier.fillMaxSize())
        }
    }

    private fun screenChild(
        route: EligibilityInnerRoute,
        componentContext: ComponentContext,
    ): ComposableContentComponent = when (route) {
        is EligibilityInnerRoute.Residency -> ResidencyListComponent(
            appComponentContext = childByContext(componentContext = componentContext, router = innerRouter),
            params = params,
            walletSelectorComponentFactory = walletSelectorComponentFactory,
        )
        is EligibilityInnerRoute.RegionUnavailable -> RegionUnavailableComponent(
            appComponentContext = childByContext(componentContext = componentContext, router = innerRouter),
            params = RegionUnavailableComponent.Params(
                userWalletId = params.userWalletId,
                product = params.product,
                countryCode = route.countryCode,
                regionCode = route.regionCode,
                onSelectAnotherRegionClick = stackNavigation::pop,
            ),
            waitlistComponentFactory = waitlistComponentFactory,
            analytics = analytics,
        )
    }

    private fun onChildBack() {
        if (childStack.value.backStack.isEmpty()) {
            router.pop()
        } else {
            stackNavigation.pop()
        }
    }

    @AssistedFactory
    interface Factory : TangemPayEligibilityComponent.Factory {
        override fun create(
            context: AppComponentContext,
            params: TangemPayEligibilityComponent.Params,
        ): DefaultTangemPayEligibilityComponent
    }
}