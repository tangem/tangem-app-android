package com.tangem.features.allservices.impl

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.arkivanov.decompose.ComponentContext
import com.arkivanov.decompose.extensions.compose.subscribeAsState
import com.arkivanov.decompose.router.slot.childSlot
import com.tangem.core.decompose.context.AppComponentContext
import com.tangem.core.decompose.context.child
import com.tangem.core.decompose.context.childByContext
import com.tangem.core.decompose.model.getOrCreateModel
import com.tangem.core.ui.decompose.ComposableBottomSheetComponent
import com.tangem.core.ui.extensions.resourceReference
import com.tangem.features.allservices.api.AllServicesComponent
import com.tangem.features.allservices.impl.entries.AllServicesSection
import com.tangem.features.allservices.impl.entries.CryptoAccountEntry
import com.tangem.features.allservices.impl.entries.JointAccountEntry
import com.tangem.features.allservices.impl.entries.StakingEntry
import com.tangem.features.allservices.impl.entries.YieldModeEntry
import com.tangem.features.allservices.impl.model.AllServicesModel
import com.tangem.features.allservices.impl.ui.AllServicesScreen
import com.tangem.features.commonfeatures.api.portfolioselector.PortfolioSelectorComponent
import com.tangem.features.introduction.AddWalletComponent
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject

/**
 * Host of the All Services screen. It decides only the sections and the order of entries in them —
 * each entry is a component that loads its own data and decides its own visibility.
 */
@Suppress("UnusedPrivateProperty")
internal class DefaultAllServicesComponent @AssistedInject constructor(
    @Assisted private val context: AppComponentContext,
    @Assisted params: Unit,
    private val portfolioSelectorComponentFactory: PortfolioSelectorComponent.Factory,
    addWalletComponentFactory: AddWalletComponent.Factory,
) : AllServicesComponent, AppComponentContext by context {

    private val model: AllServicesModel = getOrCreateModel()

    private val sections: List<AllServicesSection> = listOf(
        AllServicesSection(
            key = "account",
            title = resourceReference(R.string.common_account),
            entries = listOf(
                CryptoAccountEntry(context = child(key = CryptoAccountEntry.KEY)),
                JointAccountEntry(context = child(key = JointAccountEntry.KEY)),
            ),
        ),
        AllServicesSection(
            key = "earn",
            title = resourceReference(R.string.feed_tab_earn),
            entries = listOf(
                StakingEntry(context = child(key = StakingEntry.KEY)),
                YieldModeEntry(context = child(key = YieldModeEntry.KEY)),
            ),
        ),
    )

    private val addWalletComponent: AddWalletComponent = addWalletComponentFactory.create(
        context = child(key = "add_wallet"),
        params = AddWalletComponent.Params(callbacks = model),
    )

    private val bottomSheetSlot = childSlot(
        source = model.bottomSheetNavigation,
        serializer = null,
        handleBackButton = false,
    ) { _, componentContext -> walletSelectorChild(componentContext) }

    @Composable
    override fun Content(modifier: Modifier) {
        val state by model.uiState.collectAsStateWithLifecycle()
        val bottomSheet by bottomSheetSlot.subscribeAsState()

        Box(modifier = modifier) {
            AllServicesScreen(
                state = state,
                sections = sections,
                onAddWalletClick = addWalletComponent::open,
                modifier = Modifier.fillMaxSize(),
            )
            addWalletComponent.Content(modifier = Modifier.fillMaxSize())
        }

        bottomSheet.child?.instance?.BottomSheet()
    }

    private fun walletSelectorChild(componentContext: ComponentContext): ComposableBottomSheetComponent =
        portfolioSelectorComponentFactory.create(
            context = childByContext(componentContext),
            params = PortfolioSelectorComponent.Params(
                portfolioFetcher = model.portfolioFetcher,
                controller = model.portfolioSelectorController,
                bsCallback = model.portfolioSelectorCallback,
                settings = PortfolioSelectorComponent.Settings(isWalletSelectionOnly = true, isDs3Modal = true),
            ),
        )

    @AssistedFactory
    interface Factory : AllServicesComponent.Factory {
        override fun create(context: AppComponentContext, params: Unit): DefaultAllServicesComponent
    }
}