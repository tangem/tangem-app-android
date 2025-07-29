package com.tangem.features.hotwallet.setaccesscode

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.tangem.core.decompose.context.AppComponentContext
import com.tangem.core.decompose.model.getOrCreateModel
import com.tangem.core.ui.decompose.ComposableContentComponent
import com.tangem.core.ui.security.DisableScreenshotsDisposableEffect
import com.tangem.core.ui.extensions.stringResourceSafe
import com.tangem.features.hotwallet.setaccesscode.ui.AccessCodeLayout
import com.tangem.core.res.R
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject

internal class AccessCodeComponent @AssistedInject constructor(
    @Assisted private val context: AppComponentContext,
    @Assisted private val params: Params,
) : ComposableContentComponent, AppComponentContext by context {

    private val model: AccessCodeModel = getOrCreateModel(params)

    @Composable
    override fun Content(modifier: Modifier) {
        val state by model.uiState.collectAsStateWithLifecycle()

        DisableScreenshotsDisposableEffect()

        AccessCodeLayout(
            modifier = modifier,
            accessCode = state.accessCode,
            onAccessCodeChange = state.onAccessCodeChange,
            accessCodeLength = state.accessCodeLength,
            reEnterAccessCodeState = params.isConfirmMode,
            buttonText = stringResourceSafe(
                if (params.isConfirmMode) {
                    R.string.common_confirm
                } else {
                    R.string.common_continue
                },
            ),
            onButtonClick = state.onButtonClick,
            buttonEnabled = state.buttonEnabled,
        )
    }

    interface ModelCallbacks {
        fun onAccessCodeSet(accessCode: String)
        fun onAccessCodeConfirmed()
    }

    data class Params(
        val isConfirmMode: Boolean,
        val accessCodeToConfirm: String? = null,
        val callbacks: ModelCallbacks,
    )

    @AssistedFactory
    interface Factory {
        fun create(context: AppComponentContext, params: Params): AccessCodeComponent
    }
}