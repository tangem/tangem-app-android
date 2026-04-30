package com.tangem.features.commonfeatures.impl.choosetoken

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
import com.tangem.core.decompose.navigation.Route
import com.tangem.core.ui.decompose.ComposableBottomSheetComponent
import com.tangem.features.commonfeatures.api.choosetoken.ChooseTokenComponent
import com.tangem.features.commonfeatures.impl.choosetoken.model.ChooseTokenModel
import com.tangem.features.commonfeatures.impl.choosetoken.ui.ChooseTokenScreen
import com.tangem.features.commonfeatures.api.addtoportfolio.AddToPortfolioComponent
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import kotlinx.serialization.Serializable

internal class DefaultChooseTokenComponent @AssistedInject constructor(
    private val addToPortfolioComponentFactory: AddToPortfolioComponent.Factory,
    @Assisted appComponentContext: AppComponentContext,
    @Assisted private val params: ChooseTokenComponent.Params,
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
        val bottomSheet by bottomSheetSlot.subscribeAsState()
        val state by model.state.collectAsStateWithLifecycle()
        ChooseTokenScreen(state = state)
        bottomSheet.child?.instance?.BottomSheet()
    }

    @Suppress("UnsafeCallOnNullableType")
    private fun bottomSheetChild(componentContext: ComponentContext): ComposableBottomSheetComponent {
        return addToPortfolioComponentFactory.create(
            context = childByContext(componentContext),
            params = AddToPortfolioComponent.Params(
                addToPortfolioManager = model.addToPortfolioManager,
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

@Serializable
internal data object AddToPortfolioRoute : Route