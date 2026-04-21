package com.tangem.features.onboarding.v2.addresssync

import androidx.activity.compose.BackHandler
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.tangem.core.decompose.context.AppComponentContext
import com.tangem.core.decompose.model.getOrCreateModel
import com.tangem.features.onboarding.v2.addresssync.model.AddressSyncModel
import com.tangem.features.onboarding.v2.addresssync.ui.AddressSyncContent
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject

internal class DefaultAddressSyncComponent @AssistedInject constructor(
    @Assisted appComponentContext: AppComponentContext,
    @Assisted params: AddressSyncComponent.Params,
) : AppComponentContext by appComponentContext, AddressSyncComponent {

    private val model: AddressSyncModel = getOrCreateModel(params)

    @Composable
    override fun Content(modifier: Modifier) {
        val state by model.uiState.collectAsStateWithLifecycle()

        AddressSyncContent(modifier = modifier, state = state)

        BackHandler(onBack = state.onBackClick)
    }

    @AssistedFactory
    interface Factory : AddressSyncComponent.Factory {
        override fun create(
            context: AppComponentContext,
            params: AddressSyncComponent.Params,
        ): DefaultAddressSyncComponent
    }
}