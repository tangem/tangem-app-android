package com.tangem.features.jointaccount.supportednetworks.component

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.tangem.core.decompose.context.AppComponentContext
import com.tangem.core.decompose.model.getOrCreateModel
import com.tangem.features.jointaccount.supportednetworks.JointSupportedNetworksComponent
import com.tangem.features.jointaccount.supportednetworks.model.JointSupportedNetworksModel
import com.tangem.features.jointaccount.supportednetworks.ui.JointSupportedNetworksBottomSheet
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject

internal class DefaultJointSupportedNetworksComponent @AssistedInject constructor(
    @Assisted appComponentContext: AppComponentContext,
    @Assisted params: Unit,
) : JointSupportedNetworksComponent, AppComponentContext by appComponentContext {

    private val model: JointSupportedNetworksModel = getOrCreateModel(params = params)

    @Composable
    override fun Content(modifier: Modifier) {
        val state by model.uiState.collectAsStateWithLifecycle()

        JointSupportedNetworksBottomSheet(state = state)
    }

    @AssistedFactory
    interface Factory : JointSupportedNetworksComponent.Factory {
        override fun create(context: AppComponentContext, params: Unit): DefaultJointSupportedNetworksComponent
    }
}