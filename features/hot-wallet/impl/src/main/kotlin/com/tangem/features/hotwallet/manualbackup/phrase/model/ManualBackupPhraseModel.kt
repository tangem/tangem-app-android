package com.tangem.features.hotwallet.manualbackup.phrase.model

import androidx.compose.runtime.Stable
import com.tangem.core.decompose.di.ModelScoped
import com.tangem.core.decompose.model.Model
import com.tangem.core.decompose.model.ParamsContainer
import com.tangem.features.hotwallet.manualbackup.phrase.ManualBackupPhraseComponent
import com.tangem.features.hotwallet.manualbackup.phrase.entity.ManualBackupPhraseUM
import com.tangem.utils.coroutines.CoroutineDispatcherProvider
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import javax.inject.Inject

@Stable
@ModelScoped
internal class ManualBackupPhraseModel @Inject constructor(
    paramsContainer: ParamsContainer,
    override val dispatchers: CoroutineDispatcherProvider,
) : Model() {

    private val params = paramsContainer.require<ManualBackupPhraseComponent.Params>()
    private val callbacks = params.callbacks

    internal val uiState: StateFlow<ManualBackupPhraseUM>
    field = MutableStateFlow(getInitialUIState())

    private fun getInitialUIState(): ManualBackupPhraseUM {
        return ManualBackupPhraseUM(
            onContinueClick = callbacks::onContinueClick,
        )
    }
}