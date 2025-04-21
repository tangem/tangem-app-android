package com.tangem.features.send.v2.sendnft

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
import com.tangem.core.decompose.context.AppComponentContext
import com.tangem.core.decompose.context.childByContext
import com.tangem.core.decompose.model.getOrCreateModel
import com.tangem.core.decompose.navigation.inner.InnerRouter
import com.tangem.core.ui.decompose.ComposableContentComponent
import com.tangem.features.nft.component.NFTDetailsBlockComponent
import com.tangem.features.send.v2.api.NFTSendComponent
import com.tangem.features.send.v2.common.CommonSendRoute
import com.tangem.features.send.v2.common.ui.SendContent
import com.tangem.features.send.v2.common.ui.state.ConfirmUM
import com.tangem.features.send.v2.sendnft.confirm.NFTSendConfirmComponent
import com.tangem.features.send.v2.sendnft.model.NFTSendModel
import com.tangem.features.send.v2.subcomponents.destination.SendDestinationComponent
import com.tangem.features.send.v2.subcomponents.destination.SendDestinationComponentParams
import com.tangem.features.send.v2.subcomponents.destination.ui.state.DestinationUM
import com.tangem.features.send.v2.subcomponents.fee.SendFeeComponent
import com.tangem.features.send.v2.subcomponents.fee.SendFeeComponentParams
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.filterIsInstance
import kotlinx.coroutines.launch
import java.math.BigDecimal

internal class DefaultNFTSendComponent @AssistedInject constructor(
    @Assisted appComponentContext: AppComponentContext,
    @Assisted private val params: NFTSendComponent.Params,
    private val nftDetailsBlockComponentFactory: NFTDetailsBlockComponent.Factory,
) : NFTSendComponent, AppComponentContext by appComponentContext {

    private val stackNavigation = StackNavigation<CommonSendRoute>()

    private val innerRouter = InnerRouter<CommonSendRoute>(
        stackNavigation = stackNavigation,
        popCallback = { onChildBack() },
    )

    private val initialRoute = CommonSendRoute.Empty
    private val currentRouteFlow = MutableStateFlow<CommonSendRoute>(initialRoute)

    private val model: NFTSendModel = getOrCreateModel(params = params, router = innerRouter)

    private val childStack = childStack(
        key = "NFTSendInnerStack",
        source = stackNavigation,
        serializer = null,
        initialConfiguration = initialRoute,
        handleBackButton = true,
        childFactory = { configuration, factoryContext ->
            createChild(
                configuration,
                childByContext(
                    componentContext = factoryContext,
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
                    is NFTSendConfirmComponent -> if (currentRouteFlow.value.isEditMode) {
                        // analyticsEventHandler.send(SendAnalyticEvents.ConfirmationScreenOpened)
                        activeComponent.updateState(model.uiState.value)
                    }
                    is SendDestinationComponent -> {
                        // analyticsEventHandler.send(SendAnalyticEvents.AddressScreenOpened)
                        activeComponent.updateState(model.uiState.value.destinationUM)
                    }
                    is SendFeeComponent -> {
                        // analyticsEventHandler.send(SendAnalyticEvents.FeeScreenOpened)
                    }
                }
                currentRouteFlow.emit(stack.active.configuration)
            }
        }
    }

    @Composable
    override fun Content(modifier: Modifier) {
        val stackState by childStack.subscribeAsState()
        val state by model.uiState.collectAsStateWithLifecycle()

        BackHandler(onBack = ::onChildBack)
        SendContent(
            navigationUM = state.navigationUM,
            stackState = stackState,
        )
    }

    private fun createChild(route: CommonSendRoute, factoryContext: AppComponentContext) = when (route) {
        is CommonSendRoute.Destination -> getDestinationComponent(factoryContext, route)
        is CommonSendRoute.Fee -> getFeeComponent(factoryContext)
        CommonSendRoute.Confirm -> getConfirmComponent(factoryContext)
        else -> getStubComponent()
    }

    private fun getDestinationComponent(factoryContext: AppComponentContext, route: CommonSendRoute) =
        SendDestinationComponent(
            appComponentContext = factoryContext,
            params = SendDestinationComponentParams.DestinationParams(
                state = model.uiState.value.destinationUM,
                currentRoute = currentRouteFlow.filterIsInstance<CommonSendRoute.Destination>(),
                isBalanceHidingFlow = model.isBalanceHiddenFlow,
                analyticsCategoryName = "", // todo
                userWalletId = params.userWalletId,
                cryptoCurrency = model.cryptoCurrency,
                callback = model,
                onBackClick = ::onChildBack,
                onNextClick = {
                    if (route.isEditMode) {
                        onChildBack()
                    } else {
                        innerRouter.push(CommonSendRoute.Confirm)
                    }
                },
            ),
        )

    private fun getFeeComponent(factoryContext: AppComponentContext): ComposableContentComponent {
        val state = model.uiState.value
        val destinationAddress = (state.destinationUM as? DestinationUM.Content)?.addressTextField?.value
        return if (destinationAddress != null) {
            SendFeeComponent(
                appComponentContext = factoryContext,
                params = SendFeeComponentParams.FeeParams(
                    state = model.uiState.value.feeUM,
                    currentRoute = currentRouteFlow.filterIsInstance<CommonSendRoute.Fee>(),
                    analyticsCategoryName = "", // todo
                    userWallet = model.userWallet,
                    cryptoCurrencyStatus = model.cryptoCurrencyStatus,
                    feeCryptoCurrencyStatus = model.feeCryptoCurrencyStatus,
                    appCurrency = model.appCurrency,
                    sendAmount = BigDecimal.ZERO,
                    onLoadFee = model::loadFee,
                    destinationAddress = destinationAddress,
                    callback = model,
                    onNextClick = ::onChildBack,
                ),
            )
        } else {
            getStubComponent()
        }
    }

    private fun getConfirmComponent(factoryContext: AppComponentContext) = NFTSendConfirmComponent(
        appComponentContext = factoryContext,
        nftDetailsBlockComponentFactory = nftDetailsBlockComponentFactory,
        params = NFTSendConfirmComponent.Params(
            state = model.uiState.value,
            analyticsCategoryName = "", // todo
            userWallet = model.userWallet,
            nftAsset = params.nftAsset,
            nftCollectionName = params.nftCollectionName,
            cryptoCurrencyStatus = model.cryptoCurrencyStatus,
            feeCryptoCurrencyStatus = model.feeCryptoCurrencyStatus,
            appCurrency = model.appCurrency,
            callback = model,
            currentRoute = currentRouteFlow.filterIsInstance<CommonSendRoute.Confirm>(),
            isBalanceHidingFlow = model.isBalanceHiddenFlow,
            onLoadFee = model::loadFee,
        ),
    )

    private fun getStubComponent() = ComposableContentComponent { }

    private fun onChildBack() {
        val isEmptyRoute = childStack.value.active.configuration == CommonSendRoute.Empty
        val isEmptyStack = childStack.value.backStack.isEmpty()
        val isSuccess = model.uiState.value.confirmUM is ConfirmUM.Success

        if (isEmptyRoute || isEmptyStack || isSuccess) {
            router.pop()
        } else {
            stackNavigation.pop()
        }
    }

    @AssistedFactory
    interface Factory : NFTSendComponent.Factory {
        override fun create(context: AppComponentContext, params: NFTSendComponent.Params): DefaultNFTSendComponent
    }
}