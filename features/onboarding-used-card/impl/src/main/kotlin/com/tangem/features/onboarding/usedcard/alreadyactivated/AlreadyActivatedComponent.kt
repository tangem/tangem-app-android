package com.tangem.features.onboarding.usedcard.alreadyactivated

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.tangem.core.decompose.context.AppComponentContext
import com.tangem.core.decompose.model.getOrCreateModel
import com.tangem.core.ui.decompose.ComposableContentComponent
import com.tangem.domain.models.scan.ScanResponse
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject

internal class AlreadyActivatedComponent @AssistedInject constructor(
    @Assisted private val context: AppComponentContext,
    @Assisted private val params: Params,
) : ComposableContentComponent, AppComponentContext by context {

    private val model: AlreadyActivatedModel = getOrCreateModel(params)

    @Composable
    override fun Content(modifier: Modifier) {
        val uiState by model.uiState.collectAsStateWithLifecycle()

        AlreadyActivatedScreen(
            uiState = uiState,
            modifier = modifier,
        )
    }

    interface ModelCallback {
        fun onWalletSaved()
    }

    data class Params(
        val scanResponse: ScanResponse,
        val modelCallback: ModelCallback,
    )
}