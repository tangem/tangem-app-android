package com.tangem.features.collectibles.impl.onboarding

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.arkivanov.decompose.ComponentContext
import com.arkivanov.decompose.extensions.compose.subscribeAsState
import com.arkivanov.decompose.router.slot.childSlot
import com.tangem.core.decompose.context.AppComponentContext
import com.tangem.core.decompose.context.childByContext
import com.tangem.core.decompose.model.getOrCreateModel
import com.tangem.core.ui.decompose.ComposableBottomSheetComponent
import com.tangem.core.ui.decompose.ComposableContentComponent
import com.tangem.domain.models.wallet.UserWalletId
import com.tangem.features.collectibles.impl.common.ui.CollectiblesRegionRestrictionsSheetComponent
import com.tangem.features.collectibles.impl.onboarding.model.CollectiblesOnboardingModel
import com.tangem.features.collectibles.impl.onboarding.ui.CollectiblesOnboardingScreen
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject

/** Onboarding screen shown before the user has a Collectibles account. */
internal class CollectiblesOnboardingComponent @AssistedInject constructor(
    @Assisted context: AppComponentContext,
    @Assisted params: Params,
) : ComposableContentComponent, AppComponentContext by context {

    private val model: CollectiblesOnboardingModel = getOrCreateModel(params = params)

    private val bottomSheetSlot = childSlot(
        source = model.bottomSheetNavigation,
        serializer = CollectiblesOnboardingBottomSheetConfig.serializer(),
        key = "bottomSheet",
        handleBackButton = false,
        childFactory = ::bottomSheetChild,
    )

    @Composable
    override fun Content(modifier: Modifier) {
        val state by model.uiState.collectAsStateWithLifecycle()
        val bottomSheet by bottomSheetSlot.subscribeAsState()

        CollectiblesOnboardingScreen(state = state, modifier = modifier)

        bottomSheet.child?.instance?.BottomSheet()
    }

    private fun bottomSheetChild(
        config: CollectiblesOnboardingBottomSheetConfig,
        componentContext: ComponentContext,
    ): ComposableBottomSheetComponent = when (config) {
        CollectiblesOnboardingBottomSheetConfig.RegionRestricted -> CollectiblesRegionRestrictionsSheetComponent(
            appComponentContext = childByContext(componentContext),
            onDismiss = model::onDismissBottomSheet,
        )
    }

    data class Params(val userWalletId: UserWalletId)

    @AssistedFactory
    interface Factory {
        fun create(context: AppComponentContext, params: Params): CollectiblesOnboardingComponent
    }
}