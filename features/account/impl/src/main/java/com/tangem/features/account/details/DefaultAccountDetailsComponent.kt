package com.tangem.features.account.details

import androidx.activity.compose.BackHandler
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.arkivanov.decompose.extensions.compose.subscribeAsState
import com.arkivanov.decompose.router.slot.childSlot
import com.arkivanov.decompose.router.slot.dismiss
import com.tangem.core.decompose.context.AppComponentContext
import com.tangem.core.decompose.context.childByContext
import com.tangem.core.decompose.model.getOrCreateModel
import com.tangem.features.account.AccountDetailsComponent
import com.tangem.features.account.details.ui.AccountDetailsContent
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject

internal class DefaultAccountDetailsComponent @AssistedInject constructor(
    @Assisted appComponentContext: AppComponentContext,
    @Assisted params: AccountDetailsComponent.Params,
    private val childFactory: AccountDetailsChildFactory,
) : AppComponentContext by appComponentContext, AccountDetailsComponent {

    private val model: AccountDetailsModel = getOrCreateModel(params)

    private val bottomSheetSlot = childSlot(
        source = model.bottomSheetNavigation,
        serializer = null,
        handleBackButton = false,
        key = "bottomSheetSlot",
        childFactory = { config, componentContext ->
            childFactory.createChild(
                config = config,
                appComponentContext = childByContext(componentContext),
                model = model,
            )
        },
    )

    @Composable
    override fun Content(modifier: Modifier) {
        val state by model.uiState.collectAsStateWithLifecycle()
        val bottomSheet by bottomSheetSlot.subscribeAsState()

        BackHandler {
            if (bottomSheet.child != null) model.bottomSheetNavigation.dismiss() else state.onCloseClick()
        }
        AccountDetailsContent(
            modifier = modifier,
            state = state,
        )
        bottomSheet.child?.instance?.BottomSheet()
    }

    @AssistedFactory
    interface Factory : AccountDetailsComponent.Factory {
        override fun create(
            context: AppComponentContext,
            params: AccountDetailsComponent.Params,
        ): DefaultAccountDetailsComponent
    }
}