package com.tangem.features.managetokens.component.impl

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.tangem.core.decompose.context.AppComponentContext
import com.tangem.core.decompose.model.getOrCreateModel
import com.tangem.features.managetokens.component.ManageTokensComponent
import com.tangem.features.managetokens.model.ManageTokensModel
import com.tangem.features.managetokens.ui.ManageTokensScreen
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject

internal class DefaultManageTokensComponent @AssistedInject constructor(
    @Assisted context: AppComponentContext,
    @Assisted params: ManageTokensComponent.Params,
) : ManageTokensComponent, AppComponentContext by context {

    private val model: ManageTokensModel = getOrCreateModel(params)

    @Composable
    override fun Content(modifier: Modifier) {
        val state by model.state.collectAsStateWithLifecycle()

        ManageTokensScreen(
            modifier = modifier,
            state = state,
        )
    }

    @AssistedFactory
    interface Factory : ManageTokensComponent.Factory {
        override fun create(
            context: AppComponentContext,
            params: ManageTokensComponent.Params,
        ): DefaultManageTokensComponent
    }
}