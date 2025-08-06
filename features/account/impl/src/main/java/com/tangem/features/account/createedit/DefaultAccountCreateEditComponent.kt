package com.tangem.features.account.createedit

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.tangem.core.decompose.context.AppComponentContext
import com.tangem.core.decompose.model.getOrCreateModel
import com.tangem.features.account.AccountCreateEditComponent
import com.tangem.features.account.createedit.ui.AccountCreateEditContent
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject

internal class DefaultAccountCreateEditComponent @AssistedInject constructor(
    @Assisted appComponentContext: AppComponentContext,
    @Assisted params: AccountCreateEditComponent.Params,
) : AppComponentContext by appComponentContext, AccountCreateEditComponent {

    private val model: AccountCreateEditModel = getOrCreateModel(params)

    @Composable
    override fun Content(modifier: Modifier) {
        val state by model.uiState.collectAsStateWithLifecycle()
        AccountCreateEditContent(
            modifier = modifier,
            state = state,
        )
    }

    @AssistedFactory
    interface Factory : AccountCreateEditComponent.Factory {
        override fun create(
            context: AppComponentContext,
            params: AccountCreateEditComponent.Params,
        ): DefaultAccountCreateEditComponent
    }
}