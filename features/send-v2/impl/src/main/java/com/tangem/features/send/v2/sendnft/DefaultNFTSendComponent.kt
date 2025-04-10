package com.tangem.features.send.v2.sendnft

import androidx.activity.compose.BackHandler
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.tangem.core.decompose.context.AppComponentContext
import com.tangem.core.decompose.model.getOrCreateModel
import com.tangem.features.send.v2.api.NFTSendComponent
import com.tangem.features.send.v2.sendnft.model.NFTSendModel
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject

internal class DefaultNFTSendComponent @AssistedInject constructor(
    @Assisted appComponentContext: AppComponentContext,
    @Assisted private val params: NFTSendComponent.Params,
) : NFTSendComponent, AppComponentContext by appComponentContext {

    // private val stackNavigation = StackNavigation<NFTSendRoute>()
    //
    // private val innerRouter = InnerRouter<NFTSendRoute>(
    //     stackNavigation = stackNavigation,
    //     popCallback = { onChildBack() },
    // )

    // private val initialRoute = NFTSendRoute.Empty
    // private val currentRouteFlow = MutableStateFlow<NFTSendRoute>(initialRoute)

    private val model: NFTSendModel = getOrCreateModel(params = params/*, router = innerRouter*/)

    // private val childStack = childStack(
    //     key = "NFTSendInnerStack",
    //     source = stackNavigation,
    //     serializer = null,
    //     initialConfiguration = initialRoute,
    //     handleBackButton = true,
    //     childFactory = { configuration, factoryContext ->
    //         // todo
    //         ComposableContentComponent { }
    //     },
    // )

    init {
        // childStack.subscribe(
        //     lifecycle = lifecycle,
        //     mode = ObserveLifecycleMode.CREATE_DESTROY,
        // ) { stack ->
        //     componentScope.launch {
        //         when (val activeComponent = stack.active.instance) {
        //             is SendDestinationComponent -> {
        //                 // analyticsEventHandler.send(SendAnalyticEvents.AddressScreenOpened)
        //                 activeComponent.updateState(model.uiState.value.destinationUM)
        //             }
        //             is SendFeeComponent -> {
        //                 // analyticsEventHandler.send(SendAnalyticEvents.FeeScreenOpened)
        //             }
        //         }
        //         currentRouteFlow.emit(stack.active.configuration)
        //     }
        // }
    }

    @Composable
    override fun Content(modifier: Modifier) {
        // val stackState by childStack.subscribeAsState()
        // val state by model.uiState.collectAsStateWithLifecycle()

        BackHandler(onBack = ::onChildBack)
        // TODO
    }

    private fun onChildBack() {
        // TODO
        // val isEmptyRoute = childStack.value.active.configuration == NFTSendRoute.Empty
        // val isEmptyStack = childStack.value.backStack.isEmpty()
        // val isSuccess = model.uiState.value.confirmUM is ConfirmUM.Success
        //
        // if (isEmptyRoute || isEmptyStack || isSuccess) {
        //     router.pop()
        // } else {
        //     stackNavigation.pop()
        // }
    }

    @AssistedFactory
    interface Factory : NFTSendComponent.Factory {
        override fun create(context: AppComponentContext, params: NFTSendComponent.Params): DefaultNFTSendComponent
    }
}