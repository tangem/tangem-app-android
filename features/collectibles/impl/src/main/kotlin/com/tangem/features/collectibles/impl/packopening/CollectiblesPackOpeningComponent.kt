package com.tangem.features.collectibles.impl.packopening

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.arkivanov.decompose.ComponentContext
import com.arkivanov.decompose.extensions.compose.subscribeAsState
import com.arkivanov.decompose.router.slot.childSlot
import com.arkivanov.essenty.backhandler.BackCallback
import com.arkivanov.essenty.lifecycle.subscribe
import com.tangem.core.decompose.context.AppComponentContext
import com.tangem.core.decompose.context.childByContext
import com.tangem.core.decompose.model.getOrCreateModel
import com.tangem.core.ui.decompose.ComposableBottomSheetComponent
import com.tangem.core.ui.decompose.ComposableContentComponent
import com.tangem.core.ui.res.LocalIsInDarkTheme
import com.tangem.domain.models.wallet.UserWalletId
import com.tangem.features.collectibles.impl.packopening.model.CollectiblesPackOpeningModel
import com.tangem.features.collectibles.impl.packopening.sheet.CollectiblesConfirmOrderSheetComponent
import com.tangem.features.collectibles.impl.packopening.ui.CollectiblesPackOpeningScreen
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject

/** Order, reveal video and card share one route: a route boundary would cut the pack's throw in half. */
internal class CollectiblesPackOpeningComponent @AssistedInject constructor(
    @Assisted context: AppComponentContext,
    @Assisted params: Params,
) : ComposableContentComponent, AppComponentContext by context {

    // No state keeper: DefaultRoutingComponent does not restore the stack after process death, so it is never read
    private val model: CollectiblesPackOpeningModel = getOrCreateModel(params = params)

    private val bottomSheetSlot = childSlot(
        source = model.bottomSheetNavigation,
        serializer = CollectiblesConfirmOrderBottomSheetConfig.serializer(),
        key = "bottomSheet",
        handleBackButton = false,
        childFactory = ::bottomSheetChild,
    )

    init {
        // Takes the system back too, which would otherwise close the screen mid-purchase past the hidden back button
        backHandler.register(BackCallback { model.uiState.value.onBackClick() })
        lifecycle.subscribe(onResume = model::onResume, onPause = model::onPause)
    }

    @Composable
    override fun Content(modifier: Modifier) {
        val state by model.uiState.collectAsStateWithLifecycle()
        val bottomSheet by bottomSheetSlot.subscribeAsState()
        val isDarkTheme = LocalIsInDarkTheme.current

        LaunchedEffect(isDarkTheme) { model.onThemeChanged(isDarkTheme) }

        CollectiblesPackOpeningScreen(state = state, modifier = modifier)

        bottomSheet.child?.instance?.BottomSheet()
    }

    private fun bottomSheetChild(
        config: CollectiblesConfirmOrderBottomSheetConfig,
        componentContext: ComponentContext,
    ): ComposableBottomSheetComponent = CollectiblesConfirmOrderSheetComponent(
        appComponentContext = childByContext(componentContext),
        params = CollectiblesConfirmOrderSheetComponent.Params(
            config = config,
            onOpenPack = model::onReopenPack,
            onEnableTurboMode = model::onEnableTurboMode,
            onExplorePacks = model::onExplorePacks,
            onDismiss = model::onDismissBottomSheet,
        ),
    )

    /** @property externalId the machine's public id (e.g. `pokemon_50`), not its internal id */
    data class Params(val userWalletId: UserWalletId, val externalId: String, val isTurboEnabled: Boolean)

    @AssistedFactory
    interface Factory {
        fun create(context: AppComponentContext, params: Params): CollectiblesPackOpeningComponent
    }
}