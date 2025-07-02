package com.tangem.features.hotwallet.addexistingwallet.start

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.tangem.core.decompose.context.AppComponentContext
import com.tangem.core.decompose.model.getOrCreateModel
import com.tangem.core.ui.decompose.ComposableContentComponent
import com.tangem.features.hotwallet.addexistingwallet.start.ui.AddExistingWalletStartContent
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject

internal class AddExistingWalletStartComponent @AssistedInject constructor(
    @Assisted private val context: AppComponentContext,
    @Assisted private val params: Params,
) : ComposableContentComponent, AppComponentContext by context {
    private val model: AddExistingWalletStartModel = getOrCreateModel(params)

    @Composable
    override fun Content(modifier: Modifier) {
        val state by model.uiState.collectAsStateWithLifecycle()
        AddExistingWalletStartContent(
            state = state,
            modifier = modifier,
        )
    }

    interface ModelCallbacks {
        fun onBackClick()
        fun onImportPhraseClick()
    }

    data class Params(
        val callbacks: ModelCallbacks,
    )
}