package com.tangem.features.account.archived

import androidx.activity.compose.BackHandler
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.tangem.core.decompose.context.AppComponentContext
import com.tangem.core.decompose.model.getOrCreateModel
import com.tangem.features.account.ArchivedAccountListComponent
import com.tangem.features.account.archived.ui.ArchivedAccountListContent
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject

internal class DefaultArchivedAccountListComponent @AssistedInject constructor(
    @Assisted appComponentContext: AppComponentContext,
    @Assisted params: ArchivedAccountListComponent.Params,
) : AppComponentContext by appComponentContext, ArchivedAccountListComponent {

    private val model: ArchivedAccountListModel = getOrCreateModel(params)

    @Composable
    override fun Content(modifier: Modifier) {
        val state by model.uiState.collectAsStateWithLifecycle()
        ArchivedAccountListContent(
            modifier = modifier,
            state = state,
        )
        BackHandler(onBack = state.onCloseClick)
    }

    @AssistedFactory
    interface Factory : ArchivedAccountListComponent.Factory {
        override fun create(
            context: AppComponentContext,
            params: ArchivedAccountListComponent.Params,
        ): DefaultArchivedAccountListComponent
    }
}