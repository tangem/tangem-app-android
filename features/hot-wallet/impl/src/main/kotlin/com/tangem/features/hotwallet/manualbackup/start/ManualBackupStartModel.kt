package com.tangem.features.hotwallet.manualbackup.start

import com.tangem.core.decompose.di.ModelScoped
import com.tangem.core.decompose.model.Model
import com.tangem.core.decompose.model.ParamsContainer
import com.tangem.features.hotwallet.manualbackup.start.entity.ManualBackupStartUM
import com.tangem.utils.coroutines.CoroutineDispatcherProvider
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import javax.inject.Inject

@ModelScoped
internal class ManualBackupStartModel @Inject constructor(
    paramsContainer: ParamsContainer,
    override val dispatchers: CoroutineDispatcherProvider,
) : Model() {

    private val params: ManualBackupStartComponent.Params = paramsContainer.require()

    internal val uiState: StateFlow<ManualBackupStartUM>
    field = MutableStateFlow(
        ManualBackupStartUM(
            onContinueClick = params.callbacks::onContinueClick,
        ),
    )
}