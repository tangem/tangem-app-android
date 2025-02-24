package com.tangem.tap.features.details.ui.resetcard

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.tangem.common.routing.AppRouter
import com.tangem.core.decompose.context.AppComponentContext
import com.tangem.core.decompose.model.getOrCreateModel
import com.tangem.tap.common.extensions.dispatchNavigationAction
import com.tangem.tap.features.details.ui.resetcard.api.ResetCardComponent
import com.tangem.tap.features.details.ui.resetcard.model.ResetCardModel
import com.tangem.tap.store
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject

internal class DefaultResetCardComponent @AssistedInject constructor(
    @Assisted appComponentContext: AppComponentContext,
    @Assisted params: ResetCardComponent.Params,
) : ResetCardComponent, AppComponentContext by appComponentContext {

    private val model: ResetCardModel = getOrCreateModel(params)

    @Composable
    override fun Content(modifier: Modifier) {
        val state by model.screenState.collectAsStateWithLifecycle()

        ResetCardScreen(
            modifier = modifier,
            state = state,
            onBackClick = { store.dispatchNavigationAction(AppRouter::pop) },
        )
    }

    @AssistedFactory
    interface Factory : ResetCardComponent.Factory {
        override fun create(context: AppComponentContext, params: ResetCardComponent.Params): DefaultResetCardComponent
    }
}