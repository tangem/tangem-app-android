package com.tangem.features.send.sendnft

import androidx.activity.compose.BackHandler
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
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
import com.tangem.core.ui.decompose.ComposableModularContentComponent
import com.tangem.core.ui.extensions.resourceReference
import com.tangem.features.send.api.NFTSendComponent
import com.tangem.features.send.api.analytics.CommonSendAnalyticEvents
import com.tangem.features.send.api.subcomponents.destination.DestinationRoute
import com.tangem.features.send.api.subcomponents.destination.SendDestinationComponent
import com.tangem.features.send.api.subcomponents.destination.SendDestinationComponentParams
import com.tangem.features.send.common.CommonSendRoute
import com.tangem.features.send.common.ui.SendModularContent
import com.tangem.features.send.common.ui.state.ConfirmUM
import com.tangem.features.send.impl.R
import com.tangem.features.send.sendnft.confirm.NFTSendConfirmComponent
import com.tangem.features.send.sendnft.model.NFTSendModel
import com.tangem.features.send.sendnft.success.NFTSendSuccessComponent
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject

internal class DefaultNFTSendComponent @AssistedInject constructor(
    @Assisted appComponentContext: AppComponentContext,
    @Assisted private val params: NFTSendComponent.Params,
    private val destinationComponentFactory: SendDestinationComponent.Factory,
    private val nftSendConfirmComponentFactory: NFTSendConfirmComponent.Factory,
    private val nftSendSuccessComponentFactory: NFTSendSuccessComponent.Factory,
    private val analyticsEventHandler: AnalyticsEventHandler,
) : NFTSendComponent, AppComponentContext by appComponentContext {

    private val stackNavigation = StackNavigation<CommonSendRoute>()
    private val analyticsCategoryName = CommonSendAnalyticEvents.NFT_SEND_CATEGORY
    private val analyticsSendSource = CommonSendAnalyticEvents.CommonSendSource.NFT

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
            when (val activeComponent = stack.active.instance) {
                is NFTSendConfirmComponent -> {
                    val fromCurrency = model.cryptoCurrency
                    val fromDerivationIndex = model.account?.derivationIndex?.value
                        .takeIf { model.isAccountsMode }
                    analyticsEventHandler.send(
                        CommonSendAnalyticEvents.ConfirmationScreenOpened(
                            categoryName = analyticsCategoryName,
                            source = analyticsSendSource,
                            sendBlockchain = fromCurrency.network.name,
                            sendToken = fromCurrency.symbol,
                            fromDerivationIndex = fromDerivationIndex,
                            toDerivationIndex = null,
                        ),
                    )
                    // Push current state into a reused Confirm on (re)entry. Confirm.isEditMode is `true`
                    if (stack.active.configuration.isEditMode) {
                        activeComponent.updateState(model.uiState.value)
                    }
                }
                is SendDestinationComponent -> {
                    analyticsEventHandler.send(
                        CommonSendAnalyticEvents.AddressScreenOpened(
                            categoryName = analyticsCategoryName,
                            source = analyticsSendSource,
                        ),
                    )
                    activeComponent.updateState(model.uiState.value.destinationUM)
                }
            }
        }
    }

    @Composable
    override fun Content(modifier: Modifier) {
        val stackState by childStack.subscribeAsState()

        BackHandler(onBack = ::onChildBack)
        SendModularContent(stackState = stackState)
    }

    private fun createChild(
        route: CommonSendRoute,
        factoryContext: AppComponentContext,
    ): ComposableModularContentComponent = when (route) {
        is CommonSendRoute.Destination -> getDestinationComponent(route, factoryContext)
        CommonSendRoute.Confirm -> getConfirmComponent(factoryContext)
        CommonSendRoute.ConfirmSuccess -> getSuccessComponent(factoryContext)
        // Empty is the bootstrap placeholder until the currency status resolves; NFT has no Amount step.
        CommonSendRoute.Empty,
        is CommonSendRoute.Amount,
        -> ComposableModularContentComponent.EMPTY
    }

    private fun getDestinationComponent(
        route: DestinationRoute,
        factoryContext: AppComponentContext,
    ): ComposableModularContentComponent = destinationComponentFactory.create(
        context = factoryContext,
        params = SendDestinationComponentParams.DestinationParams(
            state = model.uiState.value.destinationUM,
            route = route,
            isBalanceHidingFlow = model.isBalanceHiddenFlow,
            title = resourceReference(R.string.nft_send),
            analyticsCategoryName = analyticsCategoryName,
            analyticsSendSource = analyticsSendSource,
            userWalletId = params.userWalletId,
            cryptoCurrency = model.cryptoCurrency,
            callback = model,
        ),
    )

    private fun getConfirmComponent(factoryContext: AppComponentContext): ComposableModularContentComponent {
        return nftSendConfirmComponentFactory.create(
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
                isBalanceHidingFlow = model.isBalanceHiddenFlow,
                onLoadFee = model::loadFee,
                analyticsSendSource = analyticsSendSource,
                account = model.account,
                isAccountsMode = model.isAccountsMode,
                onSendTransaction = { innerRouter.replaceAll(CommonSendRoute.ConfirmSuccess) },
            ),
        )
    }

    private fun getSuccessComponent(factoryContext: AppComponentContext): ComposableModularContentComponent {
        val txUrl = (model.uiState.value.confirmUM as? ConfirmUM.Success)?.txUrl

        if (txUrl == null) {
            model.showAlertError()
            return ComposableModularContentComponent.EMPTY
        }

        return nftSendSuccessComponentFactory.create(
            appComponentContext = factoryContext,
            params = NFTSendSuccessComponent.Params(
                nftSendUMFlow = model.uiState,
                analyticsCategoryName = analyticsCategoryName,
                analyticsSendSource = analyticsSendSource,
                userWallet = model.userWallet,
                cryptoCurrencyStatus = model.cryptoCurrencyStatus,
                nftAsset = params.nftAsset,
                nftCollectionName = params.nftCollectionName,
                callback = model,
                txUrl = txUrl,
                account = model.account,
                isAccountsMode = model.isAccountsMode,
            ),
        )
    }

    private fun onChildBack() {
        val isEmptyRoute = childStack.value.active.configuration == CommonSendRoute.Empty
        val isEmptyStack = childStack.value.backStack.isEmpty()
        val isSuccess = model.uiState.value.confirmUM is ConfirmUM.Success
        val isSendingInProgress = (model.uiState.value.confirmUM as? ConfirmUM.Content)?.isSending == true

        val isPopSend = isEmptyRoute || isEmptyStack || isSuccess
        when {
            isSendingInProgress -> Unit // Do not anything while transaction sending in progress
            isPopSend -> router.pop()
            else -> stackNavigation.pop()
        }
    }

    @AssistedFactory
    interface Factory : NFTSendComponent.Factory {
        override fun create(context: AppComponentContext, params: NFTSendComponent.Params): DefaultNFTSendComponent
    }
}