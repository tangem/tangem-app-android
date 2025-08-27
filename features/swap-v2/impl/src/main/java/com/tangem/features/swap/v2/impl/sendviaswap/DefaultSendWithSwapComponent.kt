package com.tangem.features.swap.v2.impl.sendviaswap

import androidx.activity.compose.BackHandler
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.arkivanov.decompose.extensions.compose.subscribeAsState
import com.arkivanov.decompose.router.stack.StackNavigation
import com.arkivanov.decompose.router.stack.childStack
import com.arkivanov.decompose.router.stack.pop
import com.arkivanov.decompose.value.ObserveLifecycleMode
import com.arkivanov.decompose.value.subscribe
import com.tangem.common.ui.navigationButtons.NavigationUM
import com.tangem.core.analytics.api.AnalyticsEventHandler
import com.tangem.core.decompose.context.AppComponentContext
import com.tangem.core.decompose.context.childByContext
import com.tangem.core.decompose.model.getOrCreateModel
import com.tangem.core.decompose.navigation.inner.InnerRouter
import com.tangem.core.ui.decompose.ComposableContentComponent
import com.tangem.core.ui.decompose.getEmptyComposableContentComponent
import com.tangem.core.ui.extensions.resourceReference
import com.tangem.domain.swap.models.R
import com.tangem.domain.swap.models.SwapDirection
import com.tangem.features.send.v2.api.analytics.CommonSendAnalyticEvents
import com.tangem.features.send.v2.api.subcomponents.destination.DestinationRoute
import com.tangem.features.send.v2.api.subcomponents.destination.SendDestinationComponent
import com.tangem.features.send.v2.api.subcomponents.destination.SendDestinationComponentParams
import com.tangem.features.swap.v2.api.SendWithSwapComponent
import com.tangem.features.swap.v2.impl.amount.SwapAmountComponent
import com.tangem.features.swap.v2.impl.amount.SwapAmountComponentParams
import com.tangem.features.swap.v2.impl.amount.entity.SwapAmountUM
import com.tangem.features.swap.v2.impl.common.SwapUtils.SEND_WITH_SWAP_PROVIDER_TYPES
import com.tangem.features.swap.v2.impl.sendviaswap.confirm.SendWithSwapConfirmComponent
import com.tangem.features.swap.v2.impl.sendviaswap.model.SendWithSwapModel
import com.tangem.features.swap.v2.impl.sendviaswap.success.SendWithSwapSuccessComponent
import com.tangem.features.swap.v2.impl.sendviaswap.ui.SendWithSwapContent
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import kotlinx.coroutines.flow.filterIsInstance
import kotlinx.coroutines.launch

internal class DefaultSendWithSwapComponent @AssistedInject constructor(
    @Assisted private val appComponentContext: AppComponentContext,
    @Assisted private val params: SendWithSwapComponent.Params,
    private val sendDestinationComponentFactory: SendDestinationComponent.Factory,
    private val confirmComponentFactory: SendWithSwapConfirmComponent.Factory,
    private val analyticsEventHandler: AnalyticsEventHandler,
) : SendWithSwapComponent, AppComponentContext by appComponentContext {

    private val stackNavigation = StackNavigation<SendWithSwapRoute>()

    private val innerRouter = InnerRouter<SendWithSwapRoute>(
        stackNavigation = stackNavigation,
        popCallback = { onChildBack() },
    )

    private val model: SendWithSwapModel = getOrCreateModel(params = params, router = innerRouter)

    private val childStack = childStack(
        key = "sendWithSwapInnerStack",
        source = stackNavigation,
        serializer = null,
        initialConfiguration = model.initialRoute,
        handleBackButton = true,
        childFactory = { configuration, componentContext ->
            createChild(
                route = configuration,
                childContext = childByContext(
                    componentContext = componentContext,
                    router = innerRouter,
                ),
            )
        },
    )

    init {
        childStack.subscribe(
            lifecycle = lifecycle,
            mode = ObserveLifecycleMode.CREATE_DESTROY,
        ) { stack ->
            componentScope.launch {
                when (val activeComponent = stack.active.instance) {
                    is SwapAmountComponent -> {
                        analyticsEventHandler.send(
                            CommonSendAnalyticEvents.AmountScreenOpened(categoryName = model.analyticCategoryName),
                        )
                        activeComponent.updateState(model.uiState.value.amountUM)
                    }
                    is SendDestinationComponent -> {
                        analyticsEventHandler.send(
                            CommonSendAnalyticEvents.AddressScreenOpened(categoryName = model.analyticCategoryName),
                        )
                        activeComponent.updateState(model.uiState.value.destinationUM)
                    }
                    is SendWithSwapConfirmComponent -> {
                        analyticsEventHandler.send(
                            CommonSendAnalyticEvents.ConfirmationScreenOpened(
                                categoryName = model.analyticCategoryName,
                            ),
                        )
                        if (model.currentRoute.value.isEditMode) {
                            activeComponent.updateState(model.uiState.value)
                        }
                    }
                }
                model.currentRoute.emit(stack.active.configuration)
            }
        }
    }

    @Composable
    override fun Content(modifier: Modifier) {
        val stackState by childStack.subscribeAsState()
        val state by model.uiState.collectAsStateWithLifecycle()

        BackHandler(
            onBack = {
                (state.navigationUM as? NavigationUM.Content)?.backIconClick() ?: onChildBack()
            },
        )
        SendWithSwapContent(navigationUM = state.navigationUM, stackState = stackState)
    }

    private fun createChild(route: SendWithSwapRoute, childContext: AppComponentContext) = when (route) {
        is SendWithSwapRoute.Amount -> getAmountComponent(factoryContext = childContext)
        is SendWithSwapRoute.Destination -> getDestinationComponent(factoryContext = childContext)
        is SendWithSwapRoute.Confirm -> getConfirmComponent(factoryContext = childContext)
        is SendWithSwapRoute.Success -> getSuccessComponent(factoryContext = childContext)
    }

    private fun getAmountComponent(factoryContext: AppComponentContext): ComposableContentComponent {
        return SwapAmountComponent(
            appComponentContext = factoryContext,
            params = SwapAmountComponentParams.AmountParams(
                amountUM = model.uiState.value.amountUM,
                title = resourceReference(R.string.common_send),
                currentRoute = model.currentRoute.filterIsInstance<SendWithSwapRoute.Amount>(),
                isBalanceHidingFlow = model.isBalanceHiddenFlow,
                analyticsCategoryName = model.analyticCategoryName,
                primaryCryptoCurrencyStatusFlow = model.primaryCryptoCurrencyStatusFlow,
                secondaryCryptoCurrency = null,
                swapDirection = SwapDirection.Direct,
                callback = model,
                userWallet = model.userWallet,
                filterProviderTypes = SEND_WITH_SWAP_PROVIDER_TYPES,
            ),
        )
    }

    private fun getDestinationComponent(factoryContext: AppComponentContext): ComposableContentComponent {
        val amountContentUM = model.uiState.value.amountUM as? SwapAmountUM.Content
            ?: return getEmptyComposableContentComponent()
        val secondaryCryptoCurrency = amountContentUM.secondaryCryptoCurrencyStatus?.currency
            ?: return getEmptyComposableContentComponent()

        return sendDestinationComponentFactory.create(
            context = factoryContext,
            params = SendDestinationComponentParams.DestinationParams(
                state = model.uiState.value.destinationUM,
                currentRoute = model.currentRoute.filterIsInstance<DestinationRoute>(),
                isBalanceHidingFlow = model.isBalanceHiddenFlow,
                analyticsCategoryName = model.analyticCategoryName,
                title = resourceReference(R.string.common_address),
                userWalletId = params.userWalletId,
                cryptoCurrency = secondaryCryptoCurrency,
                callback = model,
            ),
        )
    }

    private fun getConfirmComponent(factoryContext: AppComponentContext): ComposableContentComponent {
        return confirmComponentFactory.create(
            appComponentContext = factoryContext,
            params = SendWithSwapConfirmComponent.Params(
                sendWithSwapUM = model.uiState.value,
                currentRoute = model.currentRoute.filterIsInstance<SendWithSwapRoute.Confirm>(),
                isBalanceHidingFlow = model.isBalanceHiddenFlow,
                appCurrency = model.appCurrency,
                userWallet = model.userWallet,
                callback = model,
                analyticsCategoryName = model.analyticCategoryName,
                primaryCryptoCurrencyStatusFlow = model.primaryCryptoCurrencyStatusFlow,
                primaryFeePaidCurrencyStatusFlow = model.primaryFeePaidCurrencyStatusFlow,
                swapDirection = SwapDirection.Direct,
            ),
        )
    }

    private fun getSuccessComponent(factoryContext: AppComponentContext): ComposableContentComponent {
        return SendWithSwapSuccessComponent(
            appComponentContext = factoryContext,
            params = SendWithSwapSuccessComponent.Params(
                sendWithSwapUMFlow = model.uiState,
                currentRoute = model.currentRoute.filterIsInstance<SendWithSwapRoute.Success>(),
                callback = model,
                analyticsCategoryName = model.analyticCategoryName,
            ),
        )
    }

    private fun onChildBack() {
        val isEmptyStack = childStack.value.backStack.isEmpty()

        if (isEmptyStack) {
            router.pop()
        } else {
            stackNavigation.pop()
        }
    }

    @AssistedFactory
    interface Factory : SendWithSwapComponent.Factory {
        override fun create(
            context: AppComponentContext,
            params: SendWithSwapComponent.Params,
        ): DefaultSendWithSwapComponent
    }
}