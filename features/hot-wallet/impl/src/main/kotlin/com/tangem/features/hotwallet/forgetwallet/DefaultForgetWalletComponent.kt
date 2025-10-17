package com.tangem.features.hotwallet.forgetwallet

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.tangem.core.decompose.context.AppComponentContext
import com.tangem.core.decompose.model.getOrCreateModel
import com.tangem.features.hotwallet.ForgetWalletComponent
import com.tangem.features.hotwallet.forgetwallet.ui.ForgetWalletContent
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject

@Suppress("UnusedPrivateMember")
internal class DefaultForgetWalletComponent @AssistedInject constructor(
    @Assisted private val context: AppComponentContext,
    @Assisted private val params: ForgetWalletComponent.Params,
) : ForgetWalletComponent, AppComponentContext by context {

    private val model: ForgetWalletModel = getOrCreateModel(params)

    @Composable
    override fun Content(modifier: Modifier) {
        val state by model.uiState.collectAsStateWithLifecycle()
        ForgetWalletContent(
            state = state,
            modifier = modifier,
        )
    }

    @AssistedFactory
    interface Factory : ForgetWalletComponent.Factory {
        override fun create(
            context: AppComponentContext,
            params: ForgetWalletComponent.Params,
        ): DefaultForgetWalletComponent
    }
}