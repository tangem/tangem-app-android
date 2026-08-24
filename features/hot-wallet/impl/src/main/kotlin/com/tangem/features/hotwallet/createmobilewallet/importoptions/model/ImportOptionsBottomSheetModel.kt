package com.tangem.features.hotwallet.createmobilewallet.importoptions.model

import com.tangem.core.decompose.di.ModelScoped
import com.tangem.core.decompose.model.Model
import com.tangem.core.decompose.model.ParamsContainer
import com.tangem.core.ui.R
import com.tangem.core.ui.extensions.TextReference
import com.tangem.core.ui.extensions.resourceReference
import com.tangem.core.ui.extensions.wrappedList
import com.tangem.domain.cloudbackup.models.CloudBackupError
import com.tangem.domain.cloudbackup.models.CloudBackupInfo
import com.tangem.domain.cloudbackup.repository.CloudBackupRepository
import com.tangem.features.hotwallet.createmobilewallet.importoptions.ImportOptionsBottomSheetComponent
import com.tangem.features.hotwallet.createmobilewallet.importoptions.entity.ImportOptionsBottomSheetUM
import com.tangem.features.hotwallet.restorecloudbackup.CloudRestoreResult
import com.tangem.features.hotwallet.restorecloudbackup.CloudRestoreResultHolder
import com.tangem.utils.coroutines.CoroutineDispatcherProvider
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@ModelScoped
internal class ImportOptionsBottomSheetModel @Inject constructor(
    paramsContainer: ParamsContainer,
    override val dispatchers: CoroutineDispatcherProvider,
    private val cloudBackupRepository: CloudBackupRepository,
    private val cloudRestoreResultHolder: CloudRestoreResultHolder,
) : Model() {

    private val params = paramsContainer.require<ImportOptionsBottomSheetComponent.Params>()

    private val serviceName = resourceReference(R.string.hw_cloud_backup_service_name)

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
        modelScope.launch { resolveBackups() }
    }

    private fun isCloudLoading(): Boolean =
        (uiState.value.content as? ImportOptionsBottomSheetUM.Content.Options)?.isCloudLoading == true

    private suspend fun resolveBackups() {
        cloudBackupRepository.signOut()
        cloudBackupRepository.findBackups(interactive = true).fold(
            ifLeft = ::onLoadError,
            ifRight = { backups -> onBackupsLoaded(backups) },
        )
    }

    private suspend fun onBackupsLoaded(backups: List<CloudBackupInfo>) {
        if (backups.isEmpty()) {
            showError(
                title = resourceReference(R.string.hw_import_restore_cloud_backup_not_found, wrappedList(serviceName)),
                body = resourceReference(R.string.hw_cloud_backup_no_backups_description),
                isWarning = true,
            )
            return
        }
        val email = cloudBackupRepository.getAccountInfo(interactive = false).getOrNull()?.email
        val result = CloudRestoreResult(backups = backups, accountEmail = email)
        cloudRestoreResultHolder.set(result)
        params.onCloudBackupsResolved(result)
    }

    private fun onLoadError(error: CloudBackupError) {
        when (error) {
            CloudBackupError.AuthCanceled -> uiState.value = buildOptions(isCloudLoading = false)
            CloudBackupError.AuthPermissionsMissing,
            CloudBackupError.AuthRequired,
            -> showError(
                title = resourceReference(R.string.hw_cloud_backup_permissions_title),
                body = resourceReference(R.string.hw_cloud_backup_permissions_description),
                isWarning = false,
            )
            CloudBackupError.NetworkError -> showError(
                title = resourceReference(R.string.hw_cloud_backup_error_title),
                body = resourceReference(R.string.hw_cloud_backup_error_network),
                isWarning = false,
            )
            else -> showError(
                title = resourceReference(R.string.hw_cloud_backup_error_title),
                body = resourceReference(R.string.hw_cloud_backup_restore_error_with_recovery),
                isWarning = false,
            )
        }
    }

    private fun showError(title: TextReference, body: TextReference, isWarning: Boolean) {
        uiState.value = ImportOptionsBottomSheetUM(
            content = ImportOptionsBottomSheetUM.Content.Error(
                title = title,
                body = body,
                isWarning = isWarning,
                onGotItClick = params.onDismiss,
            ),
            onDismiss = params.onDismiss,
        )
    }
}