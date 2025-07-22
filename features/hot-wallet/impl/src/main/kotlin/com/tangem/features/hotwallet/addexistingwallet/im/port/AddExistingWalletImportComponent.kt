package com.tangem.features.hotwallet.addexistingwallet.im.port

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.tangem.core.decompose.context.AppComponentContext
import com.tangem.core.decompose.model.getOrCreateModel
import com.tangem.core.ui.decompose.ComposableContentComponent
import com.tangem.features.hotwallet.addexistingwallet.im.port.ui.AddExistingWalletImportContent
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject

internal class AddExistingWalletImportComponent @AssistedInject constructor(
    @Assisted private val context: AppComponentContext,
    @Assisted private val params: Params,
) : ComposableContentComponent, AppComponentContext by context {
    private val model: AddExistingWalletImportModel = getOrCreateModel(params)

    @Composable
    override fun Content(modifier: Modifier) {
        val state by model.uiState.collectAsStateWithLifecycle()
        AddExistingWalletImportContent(
            state = state,
            modifier = modifier,
        )
    }

    interface ModelCallbacks {
        fun onBackClick()
    }

    data class Params(
        val callbacks: ModelCallbacks,
    )
}