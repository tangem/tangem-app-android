package com.tangem.features.tokenreceive.model

import androidx.compose.runtime.Stable
import com.tangem.core.decompose.di.ModelScoped
import com.tangem.core.decompose.model.Model
import com.tangem.core.decompose.model.ParamsContainer
import com.tangem.features.tokenreceive.component.TokenReceiveWarningComponent
import com.tangem.features.tokenreceive.ui.state.WarningUM
import com.tangem.utils.coroutines.CoroutineDispatcherProvider
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import javax.inject.Inject

@Stable
@ModelScoped
internal class TokenReceiveWarningModel @Inject constructor(
    paramsContainer: ParamsContainer,
    override val dispatchers: CoroutineDispatcherProvider,
) : Model() {

    private val params = paramsContainer.require<TokenReceiveWarningComponent.TokenReceiveWarningParams>()

    internal val state: StateFlow<WarningUM>
    field = MutableStateFlow<WarningUM>(
        WarningUM(
            iconState = params.iconState,
            onWarningAcknowledged = params.callback::onWarningAcknowledged,
            network = params.network.name,
        ),
    )
}