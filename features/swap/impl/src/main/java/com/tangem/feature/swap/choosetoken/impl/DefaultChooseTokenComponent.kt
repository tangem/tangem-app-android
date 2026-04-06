package com.tangem.feature.swap.choosetoken.impl

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
import com.tangem.feature.swap.choosetoken.api.ChooseTokenComponent
import com.tangem.feature.swap.choosetoken.impl.model.ChooseTokenModel
import com.tangem.feature.swap.models.AddToPortfolioRoute
import com.tangem.feature.swap.ui.SwapSelectTokenScreen
import com.tangem.features.feed.components.market.details.portfolio.add.AddToPortfolioComponent
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject

internal class DefaultChooseTokenComponent @AssistedInject constructor(
    @Assisted appComponentContext: AppComponentContext,
    @Assisted private val params: ChooseTokenComponent.Params,
    private val addToPortfolioComponentFactory: AddToPortfolioComponent.Factory,
) : AppComponentContext by appComponentContext, ChooseTokenComponent {

    private val model: ChooseTokenModel = getOrCreateModel(params)

    private val bottomSheetSlot = childSlot(
        source = model.bottomSheetNavigation,
        serializer = AddToPortfolioRoute.serializer(),
        key = BOTTOM_SHEET_SLOT_KEY,
        handleBackButton = false,
        childFactory = { _, context -> bottomSheetChild(context) },
    )

    @Composable
    override fun Content(modifier: Modifier) {
        val stateOld by model.stateOld.collectAsStateWithLifecycle()
        val bottomSheet by bottomSheetSlot.subscribeAsState()
        stateOld?.let { stateHolder ->
            SwapSelectTokenScreen(state = stateHolder, onBack = { model.onBackClicked() })
        }
        // todo swap uncomment
        // val state by model.state.collectAsStateWithLifecycle()
        // ChooseTokenScreen(state = state)
        bottomSheet.child?.instance?.BottomSheet()
    }

    @Suppress("UnsafeCallOnNullableType")
    private fun bottomSheetChild(componentContext: ComponentContext): ComposableBottomSheetComponent {
        return addToPortfolioComponentFactory.create(
            context = childByContext(componentContext),
            params = AddToPortfolioComponent.Params(
                addToPortfolioManager = model.addToPortfolioManager!!,
                callback = model.addToPortfolioCallback,
                shouldSkipTokenActionsScreen = true,
            ),
        )
    }

    @AssistedFactory
    interface Factory : ChooseTokenComponent.Factory {
        override fun create(
            context: AppComponentContext,
            params: ChooseTokenComponent.Params,
        ): DefaultChooseTokenComponent
    }

    private companion object {
        const val BOTTOM_SHEET_SLOT_KEY = "choosePortfolioTokenBottomSheetSlot"
    }
}