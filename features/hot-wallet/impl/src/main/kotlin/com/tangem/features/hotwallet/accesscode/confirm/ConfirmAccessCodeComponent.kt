package com.tangem.features.hotwallet.accesscode.confirm

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.tangem.core.decompose.context.AppComponentContext
import com.tangem.core.decompose.model.getOrCreateModel
import com.tangem.core.ui.decompose.ComposableContentComponent
import com.tangem.core.ui.security.DisableScreenshotsDisposableEffect
import com.tangem.features.hotwallet.accesscode.confirm.ui.SetAccessCodeConfirmContent
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject

internal class ConfirmAccessCodeComponent @AssistedInject constructor(
    @Assisted private val context: AppComponentContext,
    @Assisted private val params: Params,
) : ComposableContentComponent, AppComponentContext by context {

    private val model: ConfirmAccessCodeModel = getOrCreateModel(params)

    @Composable
    override fun Content(modifier: Modifier) {
        val state by model.uiState.collectAsStateWithLifecycle()

        DisableScreenshotsDisposableEffect()

        SetAccessCodeConfirmContent(
            accessCode = state.accessCode,
            onAccessCodeChange = state.onAccessCodeChange,
            accessCodeLength = state.accessCodeLength,
            onConfirm = state.onConfirm,
            buttonEnabled = state.buttonEnabled,
            modifier = modifier,
        )
    }

    interface ModelCallbacks {
        fun onAccessCodeConfirmed()
    }

    data class Params(
        val accessCodeToConfirm: String,
        val callbacks: ModelCallbacks,
    )

    @AssistedFactory
    interface Factory {
        fun create(context: AppComponentContext, params: Params): ConfirmAccessCodeComponent
    }
}