package com.tangem.features.hotwallet.addexistingwallet.im.port

import com.tangem.core.decompose.di.ModelScoped
import com.tangem.core.decompose.model.Model
import com.tangem.core.decompose.model.ParamsContainer
import com.tangem.features.hotwallet.addexistingwallet.im.port.entity.AddExistingWalletImportUM
import com.tangem.utils.coroutines.CoroutineDispatcherProvider
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import javax.inject.Inject

@ModelScoped
internal class AddExistingWalletImportModel @Inject constructor(
    paramsContainer: ParamsContainer,
    override val dispatchers: CoroutineDispatcherProvider,
) : Model() {

    private val params: AddExistingWalletImportComponent.Params = paramsContainer.require()

    internal val uiState: StateFlow<AddExistingWalletImportUM>
    field = MutableStateFlow(
        AddExistingWalletImportUM(
            onBackClick = params.callbacks::onBackClick,
        ),
    )
}