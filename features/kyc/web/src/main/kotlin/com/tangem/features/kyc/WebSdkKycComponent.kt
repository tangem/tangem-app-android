package com.tangem.features.kyc

import androidx.activity.compose.BackHandler
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.tangem.core.decompose.context.AppComponentContext
import com.tangem.core.decompose.model.getOrCreateModel
import com.tangem.features.kyc.ui.KycWebViewScreen
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject

class WebSdkKycComponent @AssistedInject constructor(
    @Assisted appComponentContext: AppComponentContext,
    @Assisted params: KycComponent.Params,
) : KycComponent, AppComponentContext by appComponentContext {

    private val model: WebSdkKycModel = getOrCreateModel(params)

    @Composable
    override fun Content(modifier: Modifier) {
        val state by model.uiState.collectAsStateWithLifecycle()
        BackHandler(onBack = state.onBackClick)
        KycWebViewScreen(state, modifier)
    }

    @AssistedFactory
    interface Factory : KycComponent.Factory {
        override fun create(context: AppComponentContext, params: KycComponent.Params): WebSdkKycComponent
    }
}