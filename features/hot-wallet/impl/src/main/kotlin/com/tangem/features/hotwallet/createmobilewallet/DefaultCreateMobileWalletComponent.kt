package com.tangem.features.hotwallet.createmobilewallet

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.tangem.core.decompose.context.AppComponentContext
import com.tangem.core.decompose.model.getOrCreateModel
import com.tangem.features.hotwallet.CreateMobileWalletComponent
import com.tangem.features.hotwallet.createmobilewallet.ui.CreateMobileWalletContent
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject

internal class DefaultCreateMobileWalletComponent @AssistedInject constructor(
    @Assisted private val context: AppComponentContext,
    @Assisted private val params: Unit,
) : CreateMobileWalletComponent, AppComponentContext by context {
    private val model: CreateMobileWalletModel = getOrCreateModel(params)

    @Composable
    override fun Content(modifier: Modifier) {
        val state by model.uiState.collectAsStateWithLifecycle()
        CreateMobileWalletContent(
            state = state,
            modifier = modifier,
        )
    }

    @AssistedFactory
    interface Factory : CreateMobileWalletComponent.Factory {
        override fun create(context: AppComponentContext, params: Unit): DefaultCreateMobileWalletComponent
    }
}