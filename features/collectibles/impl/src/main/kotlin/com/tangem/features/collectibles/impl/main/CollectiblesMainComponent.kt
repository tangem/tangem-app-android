package com.tangem.features.collectibles.impl.main

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import com.arkivanov.decompose.ComponentContext
import com.arkivanov.decompose.extensions.compose.subscribeAsState
import com.arkivanov.decompose.router.slot.childSlot
import com.tangem.core.ui.components.containers.pullToRefresh.PullToRefreshConfig
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.tangem.core.decompose.context.AppComponentContext
import com.tangem.core.decompose.context.child
import com.tangem.core.decompose.context.childByContext
import com.tangem.core.decompose.model.getOrCreateModel
import com.tangem.core.ui.decompose.ComposableBottomSheetComponent
import com.tangem.core.ui.decompose.ComposableContentComponent
import com.tangem.domain.models.wallet.UserWalletId
import com.tangem.features.collectibles.impl.addfunds.CollectiblesAddFundsComponent
import com.tangem.features.collectibles.impl.discovery.CollectiblesDiscoveryComponent
import com.tangem.features.collectibles.impl.main.model.CollectiblesMainModel
import com.tangem.features.collectibles.impl.main.ui.CollectiblesMainScreen
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject

/** The Collectibles catalogue: balance, actions and — via the embedded discovery component — the packs on sale. */
internal class CollectiblesMainComponent @AssistedInject constructor(
    @Assisted context: AppComponentContext,
    @Assisted params: Params,
    discoveryComponentFactory: CollectiblesDiscoveryComponent.Factory,
) : ComposableContentComponent, AppComponentContext by context {

    private val model: CollectiblesMainModel = getOrCreateModel(params = params)

    private val discoveryComponent = discoveryComponentFactory.create(context = child(key = "discovery"))

    private val bottomSheetSlot = childSlot(
        source = model.bottomSheetNavigation,
        serializer = CollectiblesMainBottomSheetConfig.serializer(),
        key = "bottomSheet",
        handleBackButton = false,
        childFactory = ::bottomSheetChild,
    )

    @Composable
    override fun Content(modifier: Modifier) {
        val state by model.uiState.collectAsStateWithLifecycle()
        val discoveryState by discoveryComponent.uiState.collectAsStateWithLifecycle()
        val bottomSheet by bottomSheetSlot.subscribeAsState()

        val pullToRefresh = remember(state.isBalanceRefreshing) {
            PullToRefreshConfig(isRefreshing = state.isBalanceRefreshing, onRefresh = { onRefresh() })
        }

        CollectiblesMainScreen(
            state = state,
            pullToRefresh = pullToRefresh,
            discoveryComponent = discoveryComponent,
            discoveryState = discoveryState,
            modifier = modifier,
        )

        bottomSheet.child?.instance?.BottomSheet()
    }

    private fun bottomSheetChild(
        config: CollectiblesMainBottomSheetConfig,
        componentContext: ComponentContext,
    ): ComposableBottomSheetComponent = when (config) {
        CollectiblesMainBottomSheetConfig.TopUp -> CollectiblesAddFundsComponent(
            appComponentContext = childByContext(componentContext),
            params = CollectiblesAddFundsComponent.Params(
                callback = object : CollectiblesAddFundsComponent.Callback {
                    override fun onFromTangemWalletClick() {
                        model.onFromTangemWalletClick()
                    }

                    override fun onFromAnotherWalletClick() {
                        model.onFromAnotherWalletClick()
                    }

                    override fun onDismissAddFunds() {
                        model.onDismissAddFunds()
                    }
                },
            ),
        )
    }

    private fun onRefresh() {
        model.onRefresh()
        discoveryComponent.onRefresh()
    }

    data class Params(val userWalletId: UserWalletId)

    @AssistedFactory
    interface Factory {
        fun create(context: AppComponentContext, params: Params): CollectiblesMainComponent
    }
}