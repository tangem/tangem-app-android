package com.tangem.store.features.cardsettings.impl.securitymode

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.tangem.core.decompose.context.AppComponentContext
import com.tangem.core.decompose.model.getOrCreateModel
import com.tangem.store.features.cardsettings.impl.securitymode.api.SecurityModeComponent
import com.tangem.store.features.cardsettings.impl.securitymode.model.SecurityModeModel
import com.tangem.store.features.cardsettings.impl.securitymode.ui.SecurityModeScreen
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject

internal class DefaultSecurityModeComponent @AssistedInject constructor(
    @Assisted appComponentContext: AppComponentContext,
    @Assisted params: SecurityModeComponent.Params,
) : SecurityModeComponent, AppComponentContext by appComponentContext {

    private val model: SecurityModeModel = getOrCreateModel(params)

    @Composable
    override fun Content(modifier: Modifier) {
        val state by model.uiState.collectAsStateWithLifecycle()

        SecurityModeScreen(
            modifier = modifier,
            state = state,
            onBackClick = { router.pop() },
        )
    }

    @AssistedFactory
    interface Factory : SecurityModeComponent.Factory {
        override fun create(
            context: AppComponentContext,
            params: SecurityModeComponent.Params,
        ): DefaultSecurityModeComponent
    }
}