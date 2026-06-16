package com.tangem.features.tangempay.components

import androidx.activity.compose.BackHandler
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.tangem.core.decompose.context.AppComponentContext
import com.tangem.core.decompose.model.getOrCreateModel
import com.tangem.core.ui.decompose.ComposableContentComponent
import com.tangem.core.ui.res.LocalVisaRedesignEnabled
import com.tangem.domain.models.pay.TangemPayCard
import com.tangem.domain.models.wallet.UserWalletId
import com.tangem.features.tangempay.entity.DisplayNameState
import com.tangem.features.tangempay.model.TangemPayEditDisplayNameModel
import com.tangem.features.tangempay.ui.TangemPayEditDisplayNameScreen

internal class TangemPayEditDisplayNameComponent(
    private val appComponentContext: AppComponentContext,
    params: Params,
) : AppComponentContext by appComponentContext, ComposableContentComponent {

    private val model: TangemPayEditDisplayNameModel = getOrCreateModel(params)

    @Composable
    override fun Content(modifier: Modifier) {
        val state by model.uiState.collectAsStateWithLifecycle()
        val cardDetailsState by model.cardDetailsState.collectAsStateWithLifecycle()
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
        CompositionLocalProvider(LocalVisaRedesignEnabled provides model.isRedesignEnabled()) {
            TangemPayEditDisplayNameScreen(
                state = state,
                cardDetailsState = editingCardDetailsState,
                modifier = modifier,
                isRedesignEnabled = model.isRedesignEnabled(),
            )
        }
    }

    data class Params(val card: TangemPayCard, val userWalletId: UserWalletId)
}