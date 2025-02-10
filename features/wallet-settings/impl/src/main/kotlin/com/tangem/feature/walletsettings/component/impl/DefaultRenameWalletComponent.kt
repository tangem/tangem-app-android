package com.tangem.feature.walletsettings.component.impl

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.tangem.core.decompose.context.AppComponentContext
import com.tangem.core.decompose.model.getOrCreateModel
import com.tangem.feature.walletsettings.component.RenameWalletComponent
import com.tangem.feature.walletsettings.component.impl.model.RenameWalletModel
import com.tangem.feature.walletsettings.ui.RenameWalletDialog
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject

internal class DefaultRenameWalletComponent @AssistedInject constructor(
    @Assisted context: AppComponentContext,
    @Assisted private val params: RenameWalletComponent.Params,
) : RenameWalletComponent, AppComponentContext by context {

    private val model: RenameWalletModel = getOrCreateModel(params)

    override fun dismiss() = model.dismiss()

    @Composable
    override fun Dialog() {
        val model by model.state.collectAsStateWithLifecycle()

        RenameWalletDialog(model = model, onDismiss = ::dismiss)
    }

    @AssistedFactory
    interface Factory : RenameWalletComponent.Factory {
        override fun create(
            context: AppComponentContext,
            params: RenameWalletComponent.Params,
        ): DefaultRenameWalletComponent
    }
}