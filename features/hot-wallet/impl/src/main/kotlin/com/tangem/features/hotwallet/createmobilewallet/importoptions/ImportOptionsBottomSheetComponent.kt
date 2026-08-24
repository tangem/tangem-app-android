package com.tangem.features.hotwallet.createmobilewallet.importoptions

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.tangem.core.decompose.context.AppComponentContext
import com.tangem.core.decompose.model.getOrCreateModel
import com.tangem.core.ui.decompose.ComposableBottomSheetComponent
import com.tangem.features.hotwallet.createmobilewallet.importoptions.model.ImportOptionsBottomSheetModel
import com.tangem.features.hotwallet.createmobilewallet.importoptions.ui.ImportOptionsBottomSheet
import com.tangem.features.hotwallet.restorecloudbackup.CloudRestoreResult
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject

internal class ImportOptionsBottomSheetComponent @AssistedInject constructor(
    @Assisted appComponentContext: AppComponentContext,
    @Assisted private val params: Params,
) : ComposableBottomSheetComponent, AppComponentContext by appComponentContext {

    private val model: ImportOptionsBottomSheetModel = getOrCreateModel(params)

    override fun dismiss() {
        params.onDismiss()
    }

    @Composable
    override fun BottomSheet() {
        val state by model.uiState.collectAsStateWithLifecycle()
        ImportOptionsBottomSheet(state = state)
    }

    data class Params(
        val onRecoveryPhrase: () -> Unit,
        val onCloudBackupsResolved: (CloudRestoreResult) -> Unit,
        val onDismiss: () -> Unit,
    )

    @AssistedFactory
    interface Factory {
        fun create(context: AppComponentContext, params: Params): ImportOptionsBottomSheetComponent
    }
}

internal data object ImportOptionsBottomSheetConfig