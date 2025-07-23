package com.tangem.features.hotwallet.walletbackup.component

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.tangem.core.decompose.context.AppComponentContext
import com.tangem.core.decompose.model.getOrCreateModel
import com.tangem.features.hotwallet.WalletBackupComponent
import com.tangem.features.hotwallet.walletbackup.model.WalletBackupModel
import com.tangem.features.hotwallet.walletbackup.ui.WalletBackupContent
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject

internal class DefaultWalletBackupComponent @AssistedInject constructor(
    @Assisted private val context: AppComponentContext,
    @Assisted private val params: WalletBackupComponent.Params,
) : WalletBackupComponent, AppComponentContext by context {

    private val model: WalletBackupModel = getOrCreateModel(params)

    @Composable
    override fun Content(modifier: Modifier) {
        val state by model.uiState.collectAsStateWithLifecycle()
        WalletBackupContent(
            state = state,
            modifier = modifier,
        )
    }

    @AssistedFactory
    interface Factory : WalletBackupComponent.Factory {
        override fun create(
            context: AppComponentContext,
            params: WalletBackupComponent.Params,
        ): DefaultWalletBackupComponent
    }
}