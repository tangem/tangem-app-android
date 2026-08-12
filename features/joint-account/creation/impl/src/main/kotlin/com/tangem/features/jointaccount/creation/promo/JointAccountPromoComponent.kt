package com.tangem.features.jointaccount.creation.promo

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.tangem.core.decompose.context.AppComponentContext
import com.tangem.core.decompose.model.getOrCreateModel
import com.tangem.core.ui.decompose.ComposableContentComponent
import com.tangem.features.jointaccount.creation.component.JointAccountCreationComponent
import com.tangem.features.jointaccount.creation.promo.model.JointAccountPromoModel
import com.tangem.features.jointaccount.creation.promo.ui.JointAccountPromoScreen

internal class JointAccountPromoComponent(
    appComponentContext: AppComponentContext,
    params: JointAccountCreationComponent.Params,
) : ComposableContentComponent, AppComponentContext by appComponentContext {

    private val model: JointAccountPromoModel = getOrCreateModel(params = params)

    @Composable
    override fun Content(modifier: Modifier) {
        val state by model.uiState.collectAsStateWithLifecycle()
        JointAccountPromoScreen(state = state, modifier = modifier)
    }
}