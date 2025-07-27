package com.tangem.features.hotwallet.accesscode.set

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.tangem.core.decompose.context.AppComponentContext
import com.tangem.core.decompose.model.getOrCreateModel
import com.tangem.core.ui.decompose.ComposableContentComponent
import com.tangem.core.ui.security.DisableScreenshotsDisposableEffect
import com.tangem.features.hotwallet.accesscode.set.ui.SetAccessCodeContent
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject

internal class SetAccessCodeComponent @AssistedInject constructor(
    @Assisted private val context: AppComponentContext,
    @Assisted private val params: Params,
) : ComposableContentComponent, AppComponentContext by context {

    private val model: SetAccessCodeModel = getOrCreateModel(params)

    @Composable
    override fun Content(modifier: Modifier) {
        val state by model.uiState.collectAsStateWithLifecycle()

        DisableScreenshotsDisposableEffect()

        SetAccessCodeContent(
            accessCode = state.accessCode,
            onAccessCodeChange = state.onAccessCodeChange,
            accessCodeLength = state.accessCodeLength,
            onContinue = state.onContinue,
            buttonEnabled = state.buttonEnabled,
            modifier = modifier,
        )
    }

    interface ModelCallbacks {
        fun onAccessCodeSet(accessCode: String)
    }

    data class Params(
        val callbacks: ModelCallbacks,
    )

    @AssistedFactory
    interface Factory {
        fun create(context: AppComponentContext, params: Params): SetAccessCodeComponent
    }
}