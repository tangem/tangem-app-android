package com.tangem.features.details.component.impl

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.tangem.core.decompose.context.AppComponentContext
import com.tangem.core.decompose.model.getOrCreateModel
import com.tangem.features.details.component.UserWalletListComponent
import com.tangem.features.details.model.UserWalletListModel
import com.tangem.features.details.ui.AddWalletBottomSheet
import com.tangem.features.details.ui.UserWalletListBlock
import com.tangem.features.introduction.AddWalletComponent
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import kotlinx.coroutines.launch

internal class DefaultUserWalletListComponent @AssistedInject constructor(
    @Assisted context: AppComponentContext,
    @Assisted params: UserWalletListComponent.Params,
) : UserWalletListComponent, AppComponentContext by context {

    private val model: UserWalletListModel = getOrCreateModel()

    init {
        componentScope.launch { model.addWalletSheetRequests.collect { params.onAddWalletSheetRequest() } }
    }

    override val addWalletCallbacks: AddWalletComponent.ModelCallbacks get() = model.addWalletModelCallbacks

    @Composable
    override fun Content(modifier: Modifier) {
        val state by model.state.collectAsStateWithLifecycle()

        UserWalletListBlock(
            modifier = modifier,
            state = state,
        )

        AddWalletBottomSheet(state.addWalletBSConfig)
    }

    @AssistedFactory
    interface Factory : UserWalletListComponent.Factory {

        override fun create(
            context: AppComponentContext,
            params: UserWalletListComponent.Params,
        ): DefaultUserWalletListComponent
    }
}