package com.tangem.features.hotwallet.createmobilewallet.importoptions.model

import com.tangem.core.decompose.di.ModelScoped
import com.tangem.core.decompose.model.Model
import com.tangem.core.decompose.model.ParamsContainer
import com.tangem.domain.cloudbackup.models.CloudBackupError
import com.tangem.domain.cloudbackup.usecase.ResolveCloudRestoreBackupsUseCase
import com.tangem.features.hotwallet.cloudrestore.CloudRestoreErrorMessage
import com.tangem.features.hotwallet.createmobilewallet.importoptions.ImportOptionsBottomSheetComponent
import com.tangem.features.hotwallet.createmobilewallet.importoptions.entity.ImportOptionsBottomSheetUM
import com.tangem.utils.coroutines.CoroutineDispatcherProvider
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@ModelScoped
internal class ImportOptionsBottomSheetModel @Inject constructor(
    paramsContainer: ParamsContainer,
    override val dispatchers: CoroutineDispatcherProvider,
    private val resolveCloudRestoreBackups: ResolveCloudRestoreBackupsUseCase,
) : Model() {

    private val params = paramsContainer.require<ImportOptionsBottomSheetComponent.Params>()

    val uiState: StateFlow<ImportOptionsBottomSheetUM>
        field = MutableStateFlow(buildOptions(isCloudLoading = false))

    private fun buildOptions(isCloudLoading: Boolean): ImportOptionsBottomSheetUM = ImportOptionsBottomSheetUM(
        content = ImportOptionsBottomSheetUM.Content.Options(
            isCloudLoading = isCloudLoading,
            onRecoveryPhraseClick = ::onRecoveryPhraseClick,
            onCloudBackupClick = ::onCloudBackupClick,
        ),
        onDismiss = params.onDismiss,
    )

    private fun onRecoveryPhraseClick() {
        if (isCloudLoading()) return
        params.onRecoveryPhrase()
    }

    private fun onCloudBackupClick() {
        if (isCloudLoading()) return
        uiState.value = buildOptions(isCloudLoading = true)
        modelScope.launch {
            resolveCloudRestoreBackups().fold(
                ifLeft = ::onLoadError,
                ifRight = params.onCloudBackupsResolved,
            )
        }
    }

    private fun isCloudLoading(): Boolean =
        (uiState.value.content as? ImportOptionsBottomSheetUM.Content.Options)?.isCloudLoading == true

    private fun onLoadError(error: CloudBackupError) {
        val message = CloudRestoreErrorMessage.of(error)
        if (message == null) {
            uiState.value = buildOptions(isCloudLoading = false)
        } else {
            showError(message)
        }
    }

    private fun showError(message: CloudRestoreErrorMessage) {
        uiState.value = ImportOptionsBottomSheetUM(
            content = ImportOptionsBottomSheetUM.Content.Error(
                title = message.title,
                body = message.body,
                isWarning = message.isWarning,
                onGotItClick = params.onDismiss,
            ),
            onDismiss = params.onDismiss,
        )
    }
}