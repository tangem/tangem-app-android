package com.tangem.features.hotwallet.setaccesscode

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.tangem.core.decompose.context.AppComponentContext
import com.tangem.core.decompose.model.getOrCreateModel
import com.tangem.core.ui.decompose.ComposableContentComponent
import com.tangem.core.ui.security.DisableScreenshotsDisposableEffect
import com.tangem.features.hotwallet.setaccesscode.ui.SetAccessCodeContent
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject

internal class SetAccessCodeComponent @AssistedInject constructor(
    @Assisted private val context: AppComponentContext,
    @Assisted private val params: Params,
) : ComposableContentComponent, AppComponentContext by context {

    private val model: SetAccessCodeModel = getOrCreateModel(params)

    @Composable
    override fun Content(modifier: Modifier) {
        val state by model.uiState.collectAsStateWithLifecycle()

        SetAccessCodeContent(
            state = state,
            onBack = { model.onBack() },
            modifier = modifier,
        )

        DisableScreenshotsDisposableEffect()
    }

    interface ModelCallbacks {
        fun onBackClick()
        fun onAccessCodeSet()
    }

    data class Params(
        val callbacks: ModelCallbacks,
    )
}