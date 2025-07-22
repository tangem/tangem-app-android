package com.tangem.features.createwalletselection

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.tangem.core.decompose.context.AppComponentContext
import com.tangem.core.decompose.model.getOrCreateModel
import com.tangem.features.createwalletselection.ui.CreateWalletSelectionContent
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject

internal class DefaultCreateWalletSelectionComponent @AssistedInject constructor(
    @Assisted private val context: AppComponentContext,
    @Assisted private val params: Unit,
) : CreateWalletSelectionComponent, AppComponentContext by context {

    private val model: CreateWalletSelectionModel = getOrCreateModel(params)

    @Composable
    override fun Content(modifier: Modifier) {
        val state by model.uiState.collectAsStateWithLifecycle()
        CreateWalletSelectionContent(
            state = state,
            modifier = modifier,
        )
    }

    @AssistedFactory
    interface Factory : CreateWalletSelectionComponent.Factory {
        override fun create(context: AppComponentContext, params: Unit): DefaultCreateWalletSelectionComponent
    }
}