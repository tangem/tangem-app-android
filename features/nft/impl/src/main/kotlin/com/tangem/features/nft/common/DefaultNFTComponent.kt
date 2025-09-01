package com.tangem.features.nft.common

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
import com.tangem.core.decompose.context.AppComponentContext
import com.tangem.core.decompose.context.childByContext
import com.tangem.core.decompose.navigation.inner.InnerRouter
import com.tangem.core.ui.decompose.ComposableContentComponent
import com.tangem.features.nft.collections.NFTCollectionsComponent
import com.tangem.features.nft.common.ui.NFTContent
import com.tangem.features.nft.component.NFTComponent
import com.tangem.features.nft.details.NFTDetailsComponent
import com.tangem.features.nft.details.info.NFTDetailsInfoComponent
import com.tangem.features.nft.entity.NFTSendSuccessListener
import com.tangem.features.nft.receive.NFTReceiveComponent
import com.tangem.features.nft.traits.NFTAssetTraitsComponent
import com.tangem.features.tokenreceive.TokenReceiveComponent
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch

internal class DefaultNFTComponent @AssistedInject constructor(
    @Assisted appComponentContext: AppComponentContext,
    @Assisted private val params: NFTComponent.Params,
    private val nftDetailsInfoComponentFactory: NFTDetailsInfoComponent.Factory,
    nftSendSuccessListener: NFTSendSuccessListener,
    private val tokenReceiveComponentFactory: TokenReceiveComponent.Factory,
) : NFTComponent, AppComponentContext by appComponentContext {

    private val stackNavigation = StackNavigation<NFTRoute>()

    private val innerRouter = InnerRouter<NFTRoute>(
        stackNavigation = stackNavigation,
        popCallback = { onChildBack() },
    )

    private val initialRoute: NFTRoute = NFTRoute.Collections(params.userWalletId)
    private val currentRoute = MutableStateFlow(initialRoute)

    private val childStack = childStack(
        key = "sendInnerStack",
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
                currentRoute.emit(stack.active.configuration)
            }
        }
        nftSendSuccessListener.nftSendSuccessFlow
            .onEach {
                innerRouter.popTo(NFTRoute.Collections(userWalletId = params.userWalletId))
            }
            .launchIn(componentScope)
    }

    @Composable
    override fun Content(modifier: Modifier) {
        val stackState by childStack.subscribeAsState()

        BackHandler(onBack = ::onChildBack)
        NFTContent(
            stackState = stackState,
        )
    }

    private fun createChild(route: NFTRoute, factoryContext: AppComponentContext) = when (route) {
        is NFTRoute.Collections -> getCollectionsComponent(factoryContext, route)
        is NFTRoute.Receive -> getReceiveComponent(factoryContext, route)
        is NFTRoute.Details -> getDetailsComponent(factoryContext, route)
        is NFTRoute.AssetTraits -> getAssetTraitsComponent(factoryContext, route)
    }

    private fun getCollectionsComponent(
        factoryContext: AppComponentContext,
        route: NFTRoute.Collections,
    ): ComposableContentComponent = NFTCollectionsComponent(
        context = factoryContext,
        params = NFTCollectionsComponent.Params(
            userWalletId = route.userWalletId,
            onBackClick = ::onChildBack,
            onReceiveClick = {
                innerRouter.push(
                    NFTRoute.Receive(
                        userWalletId = route.userWalletId,
                    ),
                )
            },
            onAssetClick = { asset, collection ->
                innerRouter.push(
                    NFTRoute.Details(
                        userWalletId = route.userWalletId,
                        nftAsset = asset,
                        collection = collection,
                    ),
                )
            },
        ),
    )

    private fun getReceiveComponent(
        factoryContext: AppComponentContext,
        route: NFTRoute.Receive,
    ): ComposableContentComponent = NFTReceiveComponent(
        context = factoryContext,
        params = NFTReceiveComponent.Params(
            userWalletId = route.userWalletId,
            walletName = params.walletName,
            onBackClick = ::onChildBack,
        ),
        tokenReceiveComponentFactory = tokenReceiveComponentFactory,
    )

    private fun getDetailsComponent(
        factoryContext: AppComponentContext,
        route: NFTRoute.Details,
    ): ComposableContentComponent = NFTDetailsComponent(
        context = factoryContext,
        params = NFTDetailsComponent.Params(
            userWalletId = route.userWalletId,
            nftAsset = route.nftAsset,
            nftCollection = route.collection,
            onBackClick = ::onChildBack,
            onAllTraitsClick = {
                innerRouter.push(
                    NFTRoute.AssetTraits(
                        nftAsset = route.nftAsset,
                    ),
                )
            },
        ),
        nftDetailsInfoComponentFactory = nftDetailsInfoComponentFactory,
    )

    private fun getAssetTraitsComponent(
        factoryContext: AppComponentContext,
        route: NFTRoute.AssetTraits,
    ): ComposableContentComponent = NFTAssetTraitsComponent(
        context = factoryContext,
        params = NFTAssetTraitsComponent.Params(
            nftAsset = route.nftAsset,
            onBackClick = ::onChildBack,
        ),
    )

    private fun onChildBack() {
        val isEmptyStack = childStack.value.backStack.isEmpty()

        if (isEmptyStack) {
            router.pop()
        } else {
            stackNavigation.pop()
        }
    }

    @AssistedFactory
    interface Factory : NFTComponent.Factory {
        override fun create(context: AppComponentContext, params: NFTComponent.Params): DefaultNFTComponent
    }
}