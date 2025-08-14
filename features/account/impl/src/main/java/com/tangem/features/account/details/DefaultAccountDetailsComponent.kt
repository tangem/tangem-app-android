package com.tangem.features.account.details

import androidx.activity.compose.BackHandler
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.tangem.core.decompose.context.AppComponentContext
import com.tangem.core.decompose.model.getOrCreateModel
import com.tangem.features.account.AccountDetailsComponent
import com.tangem.features.account.details.ui.AccountDetailsContent
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject

internal class DefaultAccountDetailsComponent @AssistedInject constructor(
    @Assisted appComponentContext: AppComponentContext,
    @Assisted params: AccountDetailsComponent.Params,
) : AppComponentContext by appComponentContext, AccountDetailsComponent {

    private val model: AccountDetailsModel = getOrCreateModel(params)

    @Composable
    override fun Content(modifier: Modifier) {
        val state by model.uiState.collectAsStateWithLifecycle()
        AccountDetailsContent(
            modifier = modifier,
            state = state,
        )
        BackHandler(onBack = state.onCloseClick)
    }

    @AssistedFactory
    interface Factory : AccountDetailsComponent.Factory {
        override fun create(
            context: AppComponentContext,
            params: AccountDetailsComponent.Params,
        ): DefaultAccountDetailsComponent
    }
}