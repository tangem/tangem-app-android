package com.tangem.features.hotwallet.setupfinished

import com.tangem.core.decompose.di.ModelScoped
import com.tangem.core.decompose.model.Model
import com.tangem.core.decompose.model.ParamsContainer
import com.tangem.features.hotwallet.setupfinished.entity.MobileWalletSetupFinishedUM
import com.tangem.utils.coroutines.CoroutineDispatcherProvider
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import javax.inject.Inject

@ModelScoped
internal class MobileWalletSetupFinishedModel @Inject constructor(
    paramsContainer: ParamsContainer,
    override val dispatchers: CoroutineDispatcherProvider,
) : Model() {

    private val params: MobileWalletSetupFinishedComponent.Params = paramsContainer.require()

    internal val uiState: StateFlow<MobileWalletSetupFinishedUM>
        field = MutableStateFlow(
            MobileWalletSetupFinishedUM(
                onContinueClick = params.callbacks::onContinueClick,
            ),
        )
}