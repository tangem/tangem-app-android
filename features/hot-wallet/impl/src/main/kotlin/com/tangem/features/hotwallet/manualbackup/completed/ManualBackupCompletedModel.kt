package com.tangem.features.hotwallet.manualbackup.completed

import com.tangem.core.decompose.di.ModelScoped
import com.tangem.core.decompose.model.Model
import com.tangem.core.decompose.model.ParamsContainer
import com.tangem.features.hotwallet.manualbackup.completed.entity.ManualBackupCompletedUM
import com.tangem.utils.coroutines.CoroutineDispatcherProvider
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import javax.inject.Inject

@ModelScoped
internal class ManualBackupCompletedModel @Inject constructor(
    paramsContainer: ParamsContainer,
    override val dispatchers: CoroutineDispatcherProvider,
) : Model() {

    private val params: ManualBackupCompletedComponent.Params = paramsContainer.require()

    internal val uiState: StateFlow<ManualBackupCompletedUM>
        field = MutableStateFlow(
            ManualBackupCompletedUM(
                onContinueClick = { params.callbacks.onContinueClick(params.userWalletId) },
            ),
        )
}