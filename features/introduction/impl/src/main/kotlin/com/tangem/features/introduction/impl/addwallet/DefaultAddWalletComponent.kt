package com.tangem.features.introduction.impl.addwallet

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.tangem.core.decompose.context.AppComponentContext
import com.tangem.core.decompose.model.getOrCreateModel
import com.tangem.features.introduction.AddWalletComponent
import com.tangem.features.introduction.impl.ui.AddWalletContent
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject

internal class DefaultAddWalletComponent @AssistedInject constructor(
    @Assisted appComponentContext: AppComponentContext,
    @Assisted params: AddWalletComponent.Params,
) : AddWalletComponent, AppComponentContext by appComponentContext {

    private val model: AddWalletModel = getOrCreateModel(params)

    override fun open() = model.open()

    @Composable
    override fun Content(modifier: Modifier) {
        val state by model.uiState.collectAsStateWithLifecycle()

        AddWalletContent(state = state, modifier = modifier)
    }

    @AssistedFactory
    interface Factory : AddWalletComponent.Factory {
        override fun create(context: AppComponentContext, params: AddWalletComponent.Params): DefaultAddWalletComponent
    }
}