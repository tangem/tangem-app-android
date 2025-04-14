package com.tangem.tap.features.details.ui.securitymode

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.tangem.common.routing.AppRouter
import com.tangem.core.decompose.context.AppComponentContext
import com.tangem.core.decompose.model.getOrCreateModel
import com.tangem.tap.common.extensions.dispatchNavigationAction
import com.tangem.tap.features.details.ui.securitymode.api.SecurityModeComponent
import com.tangem.tap.features.details.ui.securitymode.model.SecurityModeModel
import com.tangem.tap.store
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
        val state by model.screenState.collectAsStateWithLifecycle()

        SecurityModeScreen(
            modifier = modifier,
            state = state,
            onBackClick = { store.dispatchNavigationAction(AppRouter::pop) },
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