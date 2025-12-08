package com.tangem.features.createwalletstart

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.tangem.core.decompose.context.AppComponentContext
import com.tangem.core.decompose.model.getOrCreateModel
import com.tangem.core.ui.components.SystemBarsIconsDisposable
import com.tangem.core.ui.res.ForceDarkTheme
import com.tangem.features.createwalletstart.ui.CreateWalletStartContent
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject

internal class DefaultCreateWalletStartComponent @AssistedInject constructor(
    @Assisted private val context: AppComponentContext,
    @Assisted private val params: CreateWalletStartComponent.Params,
) : CreateWalletStartComponent, AppComponentContext by context {

    private val model: CreateWalletStartModel = getOrCreateModel(params)

    @Composable
    override fun Content(modifier: Modifier) {
        val state by model.uiState.collectAsStateWithLifecycle()
        SystemBarsIconsDisposable(darkIcons = false)
        ForceDarkTheme {
            CreateWalletStartContent(
                state = state,
                modifier = modifier,
            )
        }
    }

    @AssistedFactory
    interface Factory : CreateWalletStartComponent.Factory {
        override fun create(
            context: AppComponentContext,
            params: CreateWalletStartComponent.Params,
        ): DefaultCreateWalletStartComponent
    }
}