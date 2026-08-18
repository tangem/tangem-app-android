package com.tangem.features.jointaccount.main.component

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.tangem.core.decompose.context.AppComponentContext
import com.tangem.core.decompose.model.getOrCreateModel
import com.tangem.features.jointaccount.main.JointAccountMembersComponent
import com.tangem.features.jointaccount.main.model.JointAccountMembersModel
import com.tangem.features.jointaccount.main.ui.JointAccountMembersScreen
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject

internal class DefaultJointAccountMembersComponent @AssistedInject constructor(
    @Assisted appComponentContext: AppComponentContext,
    @Assisted params: JointAccountMembersComponent.Params,
) : JointAccountMembersComponent, AppComponentContext by appComponentContext {

    private val model: JointAccountMembersModel = getOrCreateModel(params = params)

    @Composable
    override fun Content(modifier: Modifier) {
        val state by model.uiState.collectAsStateWithLifecycle()
        JointAccountMembersScreen(state = state, modifier = modifier)
    }

    @AssistedFactory
    interface Factory : JointAccountMembersComponent.Factory {
        override fun create(
            context: AppComponentContext,
            params: JointAccountMembersComponent.Params,
        ): DefaultJointAccountMembersComponent
    }
}