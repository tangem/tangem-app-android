package com.tangem.features.hotwallet.restorecloudbackup

import androidx.activity.compose.BackHandler
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.tangem.core.decompose.context.AppComponentContext
import com.tangem.core.decompose.model.getOrCreateModel
import com.tangem.core.ui.decompose.ComposableContentComponent
import com.tangem.core.ui.security.DisableScreenshotsDisposableEffect
import com.tangem.domain.models.wallet.UserWalletId
import com.tangem.features.hotwallet.restorecloudbackup.model.RestoreCloudBackupModel
import com.tangem.features.hotwallet.restorecloudbackup.ui.RestoreCloudBackupContent
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject

internal class RestoreCloudBackupComponent @AssistedInject constructor(
    @Assisted private val context: AppComponentContext,
    @Assisted private val params: Params,
) : ComposableContentComponent, AppComponentContext by context {

    private val model: RestoreCloudBackupModel = getOrCreateModel(params)

    @Composable
    override fun Content(modifier: Modifier) {
        val state by model.uiState.collectAsStateWithLifecycle()
        DisableScreenshotsDisposableEffect()
        BackHandler(onBack = model::onBack)
        RestoreCloudBackupContent(
            state = state,
            modifier = modifier,
        )
    }

    interface ModelCallbacks {
        fun onWalletImported(userWalletId: UserWalletId)
        fun onBack()
    }

    data class Params(
        val callbacks: ModelCallbacks,
    )
}