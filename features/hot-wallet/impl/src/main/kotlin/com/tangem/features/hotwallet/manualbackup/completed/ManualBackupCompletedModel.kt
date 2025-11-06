package com.tangem.features.hotwallet.manualbackup.completed

import com.tangem.core.decompose.di.ModelScoped
import com.tangem.core.decompose.model.Model
import com.tangem.core.decompose.model.ParamsContainer
import com.tangem.core.res.R
import com.tangem.core.ui.extensions.resourceReference
import com.tangem.features.hotwallet.manualbackup.completed.entity.ManualBackupCompletedUM
import com.tangem.utils.coroutines.CoroutineDispatcherProvider
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import javax.inject.Inject

@Suppress("LongParameterList")
@ModelScoped
internal class ManualBackupCompletedModel @Inject constructor(
    paramsContainer: ParamsContainer,
    override val dispatchers: CoroutineDispatcherProvider,
) : Model() {

    private val params: ManualBackupCompletedComponent.Params = paramsContainer.require()

    internal val uiState: StateFlow<ManualBackupCompletedUM>
        field = MutableStateFlow(
            ManualBackupCompletedUM(
                onContinueClick = {
                    if (params.isUpgradeFlow) {
                        params.callbacks.onUpgradeClick(params.userWalletId)
                    } else {
                        params.callbacks.onContinueClick(params.userWalletId)
                    }
                },
                title = if (params.isImportFlow) {
                    resourceReference(R.string.wallet_import_success_title)
                } else {
                    resourceReference(R.string.backup_complete_title)
                },
                description = resourceReference(R.string.backup_complete_description),
                isLoading = false,
            ),
        )
}