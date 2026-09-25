package com.tangem.features.hotwallet.createcloudbackup

import androidx.activity.compose.BackHandler
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.tangem.core.decompose.context.AppComponentContext
import com.tangem.core.decompose.model.getOrCreateModel
import com.tangem.core.ui.security.DisableScreenshotsDisposableEffect
import com.tangem.features.hotwallet.CreateCloudBackupComponent
import com.tangem.features.hotwallet.createcloudbackup.model.CreateCloudBackupModel
import com.tangem.features.hotwallet.createcloudbackup.ui.CreateCloudBackupContent
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject

internal class DefaultCreateCloudBackupComponent @AssistedInject constructor(
    @Assisted appComponentContext: AppComponentContext,
    @Assisted params: CreateCloudBackupComponent.Params,
) : CreateCloudBackupComponent, AppComponentContext by appComponentContext {

    private val model: CreateCloudBackupModel = getOrCreateModel(params)

    @Composable
    override fun Content(modifier: Modifier) {
        val state by model.uiState.collectAsStateWithLifecycle()

        BackHandler(onBack = model::onBack)
        // The backup password can be revealed on this screen; the restore screen already blocks capture.
        DisableScreenshotsDisposableEffect()

        CreateCloudBackupContent(
            state = state,
            modifier = modifier,
        )
    }

    @AssistedFactory
    interface Factory : CreateCloudBackupComponent.Factory {
        override fun create(
            context: AppComponentContext,
            params: CreateCloudBackupComponent.Params,
        ): DefaultCreateCloudBackupComponent
    }
}