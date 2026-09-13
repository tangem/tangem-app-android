package com.tangem.features.jointaccount.common.displayname

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.tangem.core.decompose.context.AppComponentContext
import com.tangem.core.decompose.model.getOrCreateModel
import com.tangem.features.jointaccount.common.JointAccountDisplayNameComponent
import com.tangem.features.jointaccount.common.displayname.model.JointAccountDisplayNameModel
import com.tangem.features.jointaccount.common.displayname.ui.JointAccountDisplayNameScreen
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject

internal class DefaultJointAccountDisplayNameComponent @AssistedInject constructor(
    @Assisted appComponentContext: AppComponentContext,
    @Assisted private val params: JointAccountDisplayNameComponent.Params,
) : JointAccountDisplayNameComponent, AppComponentContext by appComponentContext {

    private val model: JointAccountDisplayNameModel = getOrCreateModel(params = params)

    @Composable
    override fun Content(modifier: Modifier) {
        val state by model.uiState.collectAsStateWithLifecycle()
        JointAccountDisplayNameScreen(state = state, onCloseClick = params.onCloseClick, modifier = modifier)
    }

    @AssistedFactory
    interface Factory : JointAccountDisplayNameComponent.Factory {
        override fun create(
            context: AppComponentContext,
            params: JointAccountDisplayNameComponent.Params,
        ): DefaultJointAccountDisplayNameComponent
    }
}