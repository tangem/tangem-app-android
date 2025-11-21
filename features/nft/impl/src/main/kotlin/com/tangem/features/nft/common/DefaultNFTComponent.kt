package com.tangem.features.nft.common

import androidx.activity.compose.BackHandler
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import com.arkivanov.decompose.ComponentContext
import com.arkivanov.decompose.extensions.compose.subscribeAsState
import com.arkivanov.decompose.router.slot.SlotNavigation
import com.arkivanov.decompose.router.slot.activate
import com.arkivanov.decompose.router.slot.childSlot
import com.arkivanov.decompose.router.slot.dismiss
import com.arkivanov.decompose.router.stack.StackNavigation
import com.arkivanov.decompose.router.stack.childStack
import com.arkivanov.decompose.router.stack.pop
import com.arkivanov.decompose.value.ObserveLifecycleMode
import com.arkivanov.decompose.value.subscribe
import com.tangem.core.decompose.context.AppComponentContext
import com.tangem.core.decompose.context.childByContext
import com.tangem.core.decompose.navigation.inner.InnerRouter
import com.tangem.core.ui.decompose.ComposableBottomSheetComponent
import com.tangem.core.ui.decompose.ComposableContentComponent
import com.tangem.domain.account.featuretoggle.AccountsFeatureToggles
import com.tangem.domain.models.PortfolioId
import com.tangem.features.account.PortfolioFetcher
import com.tangem.features.account.PortfolioSelectorComponent
import com.tangem.features.account.PortfolioSelectorController
import com.tangem.features.nft.collections.NFTCollectionsComponent
import com.tangem.features.nft.common.ui.NFTContent
import com.tangem.features.nft.component.NFTComponent
import com.tangem.features.nft.details.NFTDetailsComponent
import com.tangem.features.nft.details.info.NFTDetailsInfoComponent
import com.tangem.features.nft.entity.NFTSendSuccessListener
import com.tangem.features.nft.receive.NFTReceiveComponent
import com.tangem.features.nft.traits.NFTAssetTraitsComponent
import com.tangem.features.tokenreceive.TokenReceiveComponent
import com.tangem.utils.coroutines.JobHolder
import com.tangem.utils.coroutines.saveIn
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.serialization.builtins.serializer

@Suppress("LongParameterList")
internal class DefaultNFTComponent @AssistedInject constructor(
    @Assisted appComponentContext: AppComponentContext,
    @Assisted private val params: NFTComponent.Params,
    private val nftDetailsInfoComponentFactory: NFTDetailsInfoComponent.Factory,
    nftSendSuccessListener: NFTSendSuccessListener,
    private val tokenReceiveComponentFactory: TokenReceiveComponent.Factory,
    private val portfolioSelectorComponentFactory: PortfolioSelectorComponent.Factory,
    private val portfolioSelectorController: PortfolioSelectorController,
    portfolioFetcherFactory: PortfolioFetcher.Factory,
    private val accountsFeatureToggles: AccountsFeatureToggles,
) : NFTComponent, AppComponentContext by appComponentContext {

    private val stackNavigation = StackNavigation<NFTRoute>()

    private val innerRouter = InnerRouter<NFTRoute>(
        stackNavigation = stackNavigation,
        popCallback = { onChildBack() },
    )

    private val initialRoute: NFTRoute = NFTRoute.Collections(params.userWalletId)
    private val currentRoute = MutableStateFlow(initialRoute)
    private val onReceiveClickJob = JobHolder()
    private val portfolioFetcher: PortfolioFetcher? = if (accountsFeatureToggles.isFeatureEnabled) {
        portfolioFetcherFactory.create(
            mode = PortfolioFetcher.Mode.Wallet(params.userWalletId),
            scope = componentScope,
        )
    } else {
        null
    }
    private val bottomSheetNavigation: SlotNavigation<Unit> = SlotNavigation()
    private val portfolioSelectorCallback = object : PortfolioSelectorComponent.BottomSheetCallback {
        override val onDismiss: () -> Unit = { bottomSheetNavigation.dismiss() }
        override val onBack: () -> Unit = { bottomSheetNavigation.dismiss() }
    }
    private val bottomSheetSlot = childSlot(
        source = bottomSheetNavigation,
        serializer = Unit.serializer(),
        handleBackButton = false,
        childFactory = { configuration, context -> bottomSheetChild(context) },
    )

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
        val bottomSheet by bottomSheetSlot.subscribeAsState()
        bottomSheet.child?.instance?.BottomSheet()
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
                if (accountsFeatureToggles.isFeatureEnabled) {
                    onReceiveClick(route)
                } else {
                    innerRouter.push(
                        NFTRoute.Receive(
                            portfolioId = PortfolioId(route.userWalletId),
                        ),
                    )
                }
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

    private fun onReceiveClick(route: NFTRoute.Collections) = componentScope.launch {
        val portfolioFetcher = requireNotNull(portfolioFetcher)
        portfolioSelectorController.selectAccount(null)
        portfolioFetcher.updateMode(mode = PortfolioFetcher.Mode.Wallet(route.userWalletId))
        val portfolioData = portfolioFetcher.data.first()
        if (portfolioData.isSingleChoice) {
            val mainAccountId = portfolioData.balances.values.first()
                .accountsBalance.mainAccount.account.accountId
            innerRouter.push(NFTRoute.Receive(portfolioId = PortfolioId(mainAccountId)))
        } else {
            bottomSheetNavigation.activate(Unit)
            val selectedAccountId = portfolioSelectorController.selectedAccount
                .filterNotNull().first()
            bottomSheetNavigation.dismiss()
            innerRouter.push(NFTRoute.Receive(portfolioId = PortfolioId(selectedAccountId)))
        }
    }.saveIn(onReceiveClickJob)

    private fun getReceiveComponent(
        factoryContext: AppComponentContext,
        route: NFTRoute.Receive,
    ): ComposableContentComponent = NFTReceiveComponent(
        context = factoryContext,
        params = NFTReceiveComponent.Params(
            portfolioId = route.portfolioId,
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

    private fun bottomSheetChild(componentContext: ComponentContext): ComposableBottomSheetComponent =
        portfolioSelectorComponentFactory.create(
            context = childByContext(componentContext),
            params = PortfolioSelectorComponent.Params(
                portfolioFetcher = portfolioFetcher!!,
                controller = portfolioSelectorController,
                bsCallback = portfolioSelectorCallback,
            ),
        )

    @AssistedFactory
    interface Factory : NFTComponent.Factory {
        override fun create(context: AppComponentContext, params: NFTComponent.Params): DefaultNFTComponent
    }
}