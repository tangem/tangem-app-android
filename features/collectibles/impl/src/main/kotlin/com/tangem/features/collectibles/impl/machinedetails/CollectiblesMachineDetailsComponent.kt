package com.tangem.features.collectibles.impl.machinedetails

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.arkivanov.decompose.ComponentContext
import com.arkivanov.decompose.extensions.compose.subscribeAsState
import com.arkivanov.decompose.router.slot.childSlot
import com.arkivanov.essenty.lifecycle.subscribe
import com.tangem.core.decompose.context.AppComponentContext
import com.tangem.core.decompose.context.childByContext
import com.tangem.core.decompose.model.getOrCreateModel
import com.tangem.core.ui.decompose.ComposableBottomSheetComponent
import com.tangem.core.ui.decompose.ComposableContentComponent
import com.tangem.features.collectibles.impl.machinedetails.model.CollectiblesMachineDetailsModel
import com.tangem.features.collectibles.impl.machinedetails.turboinfo.CollectiblesTurboInfoComponent
import com.tangem.features.collectibles.impl.machinedetails.ui.CollectiblesMachineDetailsScreen
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject

internal class CollectiblesMachineDetailsComponent @AssistedInject constructor(
    @Assisted context: AppComponentContext,
    @Assisted params: Params,
) : ComposableContentComponent, AppComponentContext by context {

    private val model: CollectiblesMachineDetailsModel = getOrCreateModel(params = params)

    private val bottomSheetSlot = childSlot(
        source = model.bottomSheetNavigation,
        serializer = CollectiblesMachineDetailsBottomSheetConfig.serializer(),
        key = "bottomSheet",
        handleBackButton = false,
        childFactory = ::bottomSheetChild,
    )

    init {
        lifecycle.subscribe(onResume = model::onResume, onPause = model::onPause)
    }

    @Composable
    override fun Content(modifier: Modifier) {
        val state by model.uiState.collectAsStateWithLifecycle()
        val bottomSheet by bottomSheetSlot.subscribeAsState()

        CollectiblesMachineDetailsScreen(state = state, onLoadMore = model::onLoadMore, modifier = modifier)

        bottomSheet.child?.instance?.BottomSheet()
    }

    private fun bottomSheetChild(
        config: CollectiblesMachineDetailsBottomSheetConfig,
        componentContext: ComponentContext,
    ): ComposableBottomSheetComponent = when (config) {
        CollectiblesMachineDetailsBottomSheetConfig.TurboInfo -> CollectiblesTurboInfoComponent(
            appComponentContext = childByContext(componentContext),
            params = CollectiblesTurboInfoComponent.Params(onDismiss = model::onDismissBottomSheet),
        )
    }

    data class Params(val externalId: String)

    @AssistedFactory
    interface Factory {
        fun create(context: AppComponentContext, params: Params): CollectiblesMachineDetailsComponent
    }
}