package com.tangem.features.hotwallet.manualbackup.check

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.tangem.core.decompose.context.AppComponentContext
import com.tangem.core.decompose.model.getOrCreateModel
import com.tangem.core.ui.decompose.ComposableContentComponent
import com.tangem.domain.models.wallet.UserWalletId
import com.tangem.features.hotwallet.manualbackup.check.model.ManualBackupCheckModel
import com.tangem.features.hotwallet.manualbackup.check.ui.ManualBackupCheckContent
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject

internal class ManualBackupCheckComponent @AssistedInject constructor(
    @Assisted private val context: AppComponentContext,
    @Assisted private val params: Params,
) : ComposableContentComponent, AppComponentContext by context {
    private val model: ManualBackupCheckModel = getOrCreateModel(params)

    @Composable
    override fun Content(modifier: Modifier) {
        val state by model.uiState.collectAsStateWithLifecycle()
        ManualBackupCheckContent(
            state = state,
            modifier = modifier,
        )
    }

    interface ModelCallbacks {
        fun onCompleteClick()
    }

    data class Params(
        val userWalletId: UserWalletId,
        val callbacks: ModelCallbacks,
    )
}