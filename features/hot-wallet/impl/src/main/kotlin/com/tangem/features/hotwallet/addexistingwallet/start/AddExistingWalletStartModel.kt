package com.tangem.features.hotwallet.addexistingwallet.start

import com.tangem.core.decompose.di.ModelScoped
import com.tangem.core.decompose.model.Model
import com.tangem.core.decompose.model.ParamsContainer
import com.tangem.features.hotwallet.addexistingwallet.start.entity.AddExistingWalletStartUM
import com.tangem.utils.coroutines.CoroutineDispatcherProvider
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import javax.inject.Inject

@ModelScoped
internal class AddExistingWalletStartModel @Inject constructor(
    paramsContainer: ParamsContainer,
    override val dispatchers: CoroutineDispatcherProvider,
) : Model() {

    private val params: AddExistingWalletStartComponent.Params = paramsContainer.require()

    internal val uiState: StateFlow<AddExistingWalletStartUM>
    field = MutableStateFlow(
        AddExistingWalletStartUM(
            onBackClick = params.callbacks::onBackClick,
            onImportPhraseClick = params.callbacks::onImportPhraseClick,
            onScanCardClick = { /* [REDACTED_TODO_COMMENT] */ },
            onBuyCardClick = { /* [REDACTED_TODO_COMMENT] */ },
        ),
    )
}