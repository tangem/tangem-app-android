package com.tangem.features.hotwallet.createhardwarewallet

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.tangem.core.decompose.context.AppComponentContext
import com.tangem.core.decompose.model.getOrCreateModel
import com.tangem.features.hotwallet.CreateHardwareWalletComponent
import com.tangem.features.hotwallet.createhardwarewallet.ui.CreateHardwareWalletContent
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject

@Suppress("UnusedPrivateMember")
internal class DefaultCreateHardwareWalletComponent @AssistedInject constructor(
    @Assisted private val context: AppComponentContext,
    @Assisted private val params: Unit,
) : CreateHardwareWalletComponent, AppComponentContext by context {

    private val model: CreateHardwareWalletModel = getOrCreateModel(params)

    @Composable
    override fun Content(modifier: Modifier) {
        val state by model.uiState.collectAsStateWithLifecycle()
        CreateHardwareWalletContent(
            state = state,
            modifier = modifier,
        )
    }

    @AssistedFactory
    interface Factory : CreateHardwareWalletComponent.Factory {
        override fun create(context: AppComponentContext, params: Unit): DefaultCreateHardwareWalletComponent
    }
}