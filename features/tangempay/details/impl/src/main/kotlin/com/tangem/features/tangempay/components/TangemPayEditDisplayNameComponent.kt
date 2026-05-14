package com.tangem.features.tangempay.components

import androidx.activity.compose.BackHandler
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.tangem.core.decompose.context.AppComponentContext
import com.tangem.core.decompose.context.child
import com.tangem.core.decompose.model.getOrCreateModel
import com.tangem.core.ui.decompose.ComposableContentComponent
import com.tangem.features.tangempay.components.cardDetails.DefaultTangemPayCardDetailsBlockComponent
import com.tangem.features.tangempay.components.cardDetails.TangemPayCardDetailsBlockComponent
import com.tangem.features.tangempay.entity.DisplayNameState
import com.tangem.features.tangempay.model.TangemPayEditDisplayNameModel
import com.tangem.features.tangempay.ui.TangemPayEditDisplayNameScreen

internal class TangemPayEditDisplayNameComponent(
    private val appComponentContext: AppComponentContext,
    params: TangemPayDetailsContainerComponent.Params,
) : AppComponentContext by appComponentContext, ComposableContentComponent {

    private val model: TangemPayEditDisplayNameModel = getOrCreateModel(params)

    private val cardDetailsBlockComponent = DefaultTangemPayCardDetailsBlockComponent(
        appComponentContext = child("editDisplayNameCardDetails"),
        params = TangemPayCardDetailsBlockComponent.Params(params = params, isEditingNameEnabled = false),
    )

    @Composable
    override fun Content(modifier: Modifier) {
        val state by model.uiState.collectAsStateWithLifecycle()
        val cardDetailsState by cardDetailsBlockComponent.state.collectAsStateWithLifecycle()
        val editingCardDetailsState = cardDetailsState.copy(
            displayNameState = DisplayNameState.Editing(
                displayName = state.editingValue.text,
                editingValue = state.editingValue,
                isSubmitEnabled = state.isDoneEnabled,
                onValueChanged = state.onValueChanged,
                onSubmit = state.onDoneClick,
                onDismiss = state.onDismiss,
            ),
        )
        BackHandler(onBack = state.onDismiss)
        TangemPayEditDisplayNameScreen(
            state = state,
            cardDetailsBlockComponent = cardDetailsBlockComponent,
            cardDetailsState = editingCardDetailsState,
            modifier = modifier,
        )
    }
}