package com.tangem.features.jointaccount.creation.config

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.tangem.core.decompose.context.AppComponentContext
import com.tangem.core.decompose.model.getOrCreateModel
import com.tangem.core.ui.decompose.ComposableContentComponent
import com.tangem.features.jointaccount.creation.component.JointAccountCreationComponent
import com.tangem.features.jointaccount.creation.config.model.JointAccountConfigModel
import com.tangem.features.jointaccount.creation.config.ui.JointAccountConfigScreen

internal class JointAccountConfigComponent(
    appComponentContext: AppComponentContext,
    params: JointAccountCreationComponent.Params,
    private val onCloseClick: () -> Unit,
) : ComposableContentComponent, AppComponentContext by appComponentContext {

    private val model: JointAccountConfigModel = getOrCreateModel(params = params)

    @Composable
    override fun Content(modifier: Modifier) {
        val state by model.uiState.collectAsStateWithLifecycle()
        JointAccountConfigScreen(state = state, onCloseClick = onCloseClick, modifier = modifier)
    }
}