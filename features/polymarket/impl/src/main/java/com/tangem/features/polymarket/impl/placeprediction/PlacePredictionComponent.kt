package com.tangem.features.polymarket.impl.placeprediction

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
import com.tangem.core.decompose.context.AppComponentContext
import com.tangem.core.decompose.context.childByContext
import com.tangem.core.decompose.model.getOrCreateModel
import com.tangem.core.decompose.navigation.inner.InnerRouter
import com.tangem.core.ui.decompose.ComposableContentComponent
import com.tangem.domain.models.wallet.UserWalletId
import com.tangem.domain.polymarket.model.PredictionOrderSide
import com.tangem.features.polymarket.impl.placeprediction.amount.PlacePredictionAmountComponent
import com.tangem.features.polymarket.impl.placeprediction.model.PlacePredictionModel
import com.tangem.features.polymarket.impl.placeprediction.status.PlacePredictionStatusComponent
import com.tangem.features.polymarket.impl.placeprediction.summary.PlacePredictionSummaryComponent

/**
 * Root of the place-prediction flow: amount → summary → status.
 *

 * route in the Polymarket feature stack.
 */
internal class PlacePredictionComponent(
    appComponentContext: AppComponentContext,
    private val params: Params,
) : ComposableContentComponent, AppComponentContext by appComponentContext {

    private val stackNavigation = StackNavigation<PlacePredictionRoute>()

    private val innerRouter = InnerRouter<PlacePredictionRoute>(
        stackNavigation = stackNavigation,
        popCallback = { onChildBack() },
    )

    /**

     * resolving its own would get a second, empty flow state rather than this one.
     */
    private val model: PlacePredictionModel = getOrCreateModel(params = params, router = innerRouter)

    private val childStack = childStack(
        key = "placePredictionStack",
        source = stackNavigation,
        serializer = null,
        initialConfiguration = PlacePredictionRoute.Amount,
        handleBackButton = true,
        childFactory = { configuration, factoryContext ->
            getChildComponent(
                configuration = configuration,
                factoryContext = childByContext(componentContext = factoryContext, router = innerRouter),
            )
        },
    )

    @Composable
    override fun Content(modifier: Modifier) {
        val stackValue by childStack.subscribeAsState()

        Children(
            stack = stackValue,
            modifier = modifier,
            animation = stackAnimation { slide() },
        ) { child ->
            child.instance.Content(Modifier.fillMaxSize())
        }
    }

    private fun getChildComponent(
        configuration: PlacePredictionRoute,
        factoryContext: AppComponentContext,
    ): ComposableContentComponent = when (configuration) {
        is PlacePredictionRoute.Amount -> PlacePredictionAmountComponent(factoryContext, model)
        is PlacePredictionRoute.Summary -> PlacePredictionSummaryComponent(factoryContext, model)
        is PlacePredictionRoute.Status -> PlacePredictionStatusComponent(factoryContext, model)
    }

    private fun onChildBack() {
        if (childStack.value.backStack.isEmpty()) {
            router.pop()
        } else {
            stackNavigation.pop()
        }
    }

    /**
     * @property userWalletId wallet the order is placed from
     * @property eventId event the prediction belongs to
     * @property marketId market inside the event
     * @property assetId outcome the user tapped
     * @property side direction of the order
     *
     * Carries no access mode: whether trading is allowed is produced by the region, which a route and its
     * params cannot know — the flow reads it itself, where the answer means something.
     */
    data class Params(
        val userWalletId: UserWalletId,
        val eventId: String,
        val marketId: String,
        val assetId: String,
        val side: PredictionOrderSide,
    )
}