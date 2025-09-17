package com.tangem.features.hotwallet.manualbackup.phrase

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.tangem.core.decompose.context.AppComponentContext
import com.tangem.core.decompose.model.getOrCreateModel
import com.tangem.core.ui.decompose.ComposableContentComponent
import com.tangem.core.ui.security.DisableScreenshotsDisposableEffect
import com.tangem.domain.models.wallet.UserWalletId
import com.tangem.features.hotwallet.manualbackup.phrase.model.ManualBackupPhraseModel
import com.tangem.features.hotwallet.manualbackup.phrase.ui.ManualBackupPhraseContent
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject

internal class ManualBackupPhraseComponent @AssistedInject constructor(
    @Assisted private val context: AppComponentContext,
    @Assisted private val params: Params,
) : ComposableContentComponent, AppComponentContext by context {
    private val model: ManualBackupPhraseModel = getOrCreateModel(params)

    @Composable
    override fun Content(modifier: Modifier) {
        val state by model.uiState.collectAsStateWithLifecycle()
        DisableScreenshotsDisposableEffect()
        ManualBackupPhraseContent(
            state = state,
            modifier = modifier,
        )
    }

    interface ModelCallbacks {
        fun onContinueClick()
    }

    data class Params(
        val userWalletId: UserWalletId,
        val callbacks: ModelCallbacks,
    )
}