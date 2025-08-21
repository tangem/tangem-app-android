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
import com.tangem.core.analytics.api.AnalyticsEventHandler
import com.tangem.core.decompose.context.AppComponentContext
import com.tangem.core.decompose.context.childByContext
import com.tangem.core.decompose.model.getOrCreateModel
import com.tangem.core.decompose.navigation.inner.InnerRouter
import com.tangem.core.ui.decompose.ComposableContentComponent
import com.tangem.core.ui.extensions.resourceReference
import com.tangem.features.send.v2.api.NFTSendComponent
import com.tangem.features.send.v2.api.analytics.CommonSendAnalyticEvents
import com.tangem.features.send.v2.api.subcomponents.destination.SendDestinationComponentParams
import com.tangem.features.send.v2.api.subcomponents.destination.entity.DestinationUM
import com.tangem.features.send.v2.common.CommonSendRoute
import com.tangem.features.send.v2.common.ui.SendContent
import com.tangem.features.send.v2.common.ui.state.ConfirmUM
import com.tangem.features.send.v2.impl.R
import com.tangem.features.send.v2.sendnft.confirm.NFTSendConfirmComponent
import com.tangem.features.send.v2.sendnft.model.NFTSendModel
import com.tangem.features.send.v2.sendnft.success.NFTSendSuccessComponent
import com.tangem.features.send.v2.subcomponents.destination.DefaultSendDestinationComponent
import com.tangem.features.send.v2.subcomponents.fee.SendFeeComponent
import com.tangem.features.send.v2.subcomponents.fee.SendFeeComponentParams
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import kotlinx.coroutines.flow.filterIsInstance
import kotlinx.coroutines.launch
import java.math.BigDecimal

internal class DefaultNFTSendComponent @AssistedInject constructor(
    @Assisted appComponentContext: AppComponentContext,
    @Assisted private val params: NFTSendComponent.Params,
    private val nftSendConfirmComponentFactory: NFTSendConfirmComponent.Factory,
    private val nftSendSuccessComponentFactory: NFTSendSuccessComponent.Factory,
    private val analyticsEventHandler: AnalyticsEventHandler,
) : NFTSendComponent, AppComponentContext by appComponentContext {

    private val stackNavigation = StackNavigation<CommonSendRoute>()
    private val analyticsCategoryName = CommonSendAnalyticEvents.NFT_SEND_CATEGORY

    private val innerRouter = InnerRouter<CommonSendRoute>(
        stackNavigation = stackNavigation,
        popCallback = { onChildBack() },
    )

    private val model: NFTSendModel = getOrCreateModel(params = params, router = innerRouter)

    private val childStack = childStack(
        key = "NFTSendInnerStack",
        source = stackNavigation,
        serializer = null,
        initialConfiguration = model.initialRoute,
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
                    is NFTSendConfirmComponent -> {
                        analyticsEventHandler.send(
                            CommonSendAnalyticEvents.ConfirmationScreenOpened(categoryName = analyticsCategoryName),
                        )
                        if (model.currentRouteFlow.value.isEditMode) {
                            activeComponent.updateState(model.uiState.value)
                        }
                    }
                    is DefaultSendDestinationComponent -> {
                        analyticsEventHandler.send(
                            CommonSendAnalyticEvents.AddressScreenOpened(categoryName = analyticsCategoryName),
                        )
                        activeComponent.updateState(model.uiState.value.destinationUM)
                    }
                    is SendFeeComponent -> {
                        analyticsEventHandler.send(
                            CommonSendAnalyticEvents.FeeScreenOpened(categoryName = analyticsCategoryName),
                        )
                    }
                }
                model.currentRouteFlow.emit(stack.active.configuration)
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
        is CommonSendRoute.Destination -> getDestinationComponent(factoryContext)
        is CommonSendRoute.Fee -> getFeeComponent(factoryContext)
        CommonSendRoute.Confirm -> getConfirmComponent(factoryContext)
        CommonSendRoute.ConfirmSuccess -> getSuccessComponent(factoryContext)
        else -> getStubComponent()
    }

    private fun getDestinationComponent(factoryContext: AppComponentContext): DefaultSendDestinationComponent =
        DefaultSendDestinationComponent(
            appComponentContext = factoryContext,
            params = SendDestinationComponentParams.DestinationParams(
                state = model.uiState.value.destinationUM,
                currentRoute = model.currentRouteFlow.filterIsInstance<CommonSendRoute.Destination>(),
                isBalanceHidingFlow = model.isBalanceHiddenFlow,
                title = resourceReference(R.string.nft_send),
                analyticsCategoryName = analyticsCategoryName,
                userWalletId = params.userWalletId,
                cryptoCurrency = model.cryptoCurrency,
                callback = model,
            ),
        )

    private fun getFeeComponent(factoryContext: AppComponentContext): ComposableContentComponent {
        val state = model.uiState.value
        val destinationAddress = (state.destinationUM as? DestinationUM.Content)?.addressTextField?.actualAddress
        return if (destinationAddress != null) {
            SendFeeComponent(
                appComponentContext = factoryContext,
                params = SendFeeComponentParams.FeeParams(
                    state = model.uiState.value.feeUM,
                    currentRoute = model.currentRouteFlow.filterIsInstance<CommonSendRoute.Fee>(),
                    analyticsCategoryName = analyticsCategoryName,
                    userWallet = model.userWallet,
                    cryptoCurrencyStatus = model.cryptoCurrencyStatus,
                    feeCryptoCurrencyStatus = model.feeCryptoCurrencyStatus,
                    appCurrency = model.appCurrency,
                    sendAmount = BigDecimal.ZERO,
                    onLoadFee = model::loadFee,
                    destinationAddress = destinationAddress,
                    callback = model,
                ),
            )
        } else {
            getStubComponent()
        }
    }

    private fun getConfirmComponent(factoryContext: AppComponentContext) = nftSendConfirmComponentFactory.create(
        appComponentContext = factoryContext,
        params = NFTSendConfirmComponent.Params(
            state = model.uiState.value,
            analyticsCategoryName = analyticsCategoryName,
            userWallet = model.userWallet,
            nftAsset = params.nftAsset,
            nftCollectionName = params.nftCollectionName,
            cryptoCurrencyStatus = model.cryptoCurrencyStatus,
            feeCryptoCurrencyStatus = model.feeCryptoCurrencyStatus,
            appCurrency = model.appCurrency,
            callback = model,
            currentRoute = model.currentRouteFlow.filterIsInstance<CommonSendRoute.Confirm>(),
            isBalanceHidingFlow = model.isBalanceHiddenFlow,
            onLoadFee = model::loadFee,
            onSendTransaction = { innerRouter.replaceAll(CommonSendRoute.ConfirmSuccess) },
        ),
    )

    private fun getSuccessComponent(factoryContext: AppComponentContext): ComposableContentComponent {
        val txUrl = (model.uiState.value.confirmUM as? ConfirmUM.Success)?.txUrl

        if (txUrl == null) {
            model.showAlertError()
            return getStubComponent()
        }

        return nftSendSuccessComponentFactory.create(
            appComponentContext = factoryContext,
            params = NFTSendSuccessComponent.Params(
                nftSendUMFlow = model.uiState,
                analyticsCategoryName = analyticsCategoryName,
                userWallet = model.userWallet,
                cryptoCurrencyStatus = model.cryptoCurrencyStatus,
                nftAsset = params.nftAsset,
                nftCollectionName = params.nftCollectionName,
                callback = model,
                currentRoute = model.currentRouteFlow.filterIsInstance<CommonSendRoute.ConfirmSuccess>(),
                txUrl = txUrl,
            ),
        )
    }

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