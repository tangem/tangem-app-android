package com.tangem.features.jointaccount.creation.composition

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.tangem.core.decompose.context.AppComponentContext
import com.tangem.core.decompose.model.getOrCreateModel
import com.tangem.core.ui.decompose.ComposableContentComponent
import com.tangem.features.jointaccount.creation.composition.model.JointAccountCompositionModel
import com.tangem.features.jointaccount.creation.composition.ui.JointAccountCompositionScreen
import com.tangem.features.jointaccount.creation.model.JointAccountCreationChildParams

internal class JointAccountCompositionComponent(
    appComponentContext: AppComponentContext,
    params: JointAccountCreationChildParams,
    private val onCloseClick: () -> Unit,
) : ComposableContentComponent, AppComponentContext by appComponentContext {

    private val model: JointAccountCompositionModel = getOrCreateModel(params = params)

    @Composable
    override fun Content(modifier: Modifier) {
        val state by model.uiState.collectAsStateWithLifecycle()
        JointAccountCompositionScreen(state = state, onCloseClick = onCloseClick, modifier = modifier)
    }
}