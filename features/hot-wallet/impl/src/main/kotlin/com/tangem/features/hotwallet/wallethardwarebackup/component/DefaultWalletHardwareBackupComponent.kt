package com.tangem.features.hotwallet.wallethardwarebackup.component

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.tangem.core.decompose.context.AppComponentContext
import com.tangem.core.decompose.model.getOrCreateModel
import com.tangem.features.hotwallet.WalletHardwareBackupComponent
import com.tangem.features.hotwallet.wallethardwarebackup.model.WalletHardwareBackupModel
import com.tangem.features.hotwallet.wallethardwarebackup.ui.WalletHardwareBackupContent
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject

internal class DefaultWalletHardwareBackupComponent @AssistedInject constructor(
    @Assisted private val context: AppComponentContext,
    @Assisted private val params: WalletHardwareBackupComponent.Params,
) : WalletHardwareBackupComponent, AppComponentContext by context {

    private val model: WalletHardwareBackupModel = getOrCreateModel(params)

    @Composable
    override fun Content(modifier: Modifier) {
        val state by model.uiState.collectAsStateWithLifecycle()
        WalletHardwareBackupContent(
            state = state,
            modifier = modifier,
        )
    }

    @AssistedFactory
    interface Factory : WalletHardwareBackupComponent.Factory {
        override fun create(
            context: AppComponentContext,
            params: WalletHardwareBackupComponent.Params,
        ): DefaultWalletHardwareBackupComponent
    }
}