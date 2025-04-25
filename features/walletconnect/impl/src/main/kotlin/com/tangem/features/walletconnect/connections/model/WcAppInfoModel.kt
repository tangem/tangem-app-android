package com.tangem.features.walletconnect.connections.model

import androidx.compose.runtime.Stable
import com.tangem.core.decompose.di.ModelScoped
import com.tangem.core.decompose.model.Model
import com.tangem.core.decompose.model.ParamsContainer
import com.tangem.features.walletconnect.connections.components.WcAppInfoContainerComponent
import com.tangem.features.walletconnect.connections.entity.WcAppInfoUM
import com.tangem.features.walletconnect.connections.entity.WcPrimaryButtonConfig
import com.tangem.utils.coroutines.CoroutineDispatcherProvider
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import javax.inject.Inject

@Stable
@ModelScoped
internal class WcAppInfoModel @Inject constructor(
    override val dispatchers: CoroutineDispatcherProvider,
    paramsContainer: ParamsContainer,
) : Model() {

    private val params: WcAppInfoContainerComponent.Params = paramsContainer.require()
    val appInfoUiState: StateFlow<WcAppInfoUM>
    field = MutableStateFlow<WcAppInfoUM>(createLoadingState())

    fun dismiss() {
        params.onDismiss()
    }

    private fun onConnect() {
// [REDACTED_TODO_COMMENT]
    }

    private fun createLoadingState(): WcAppInfoUM.Loading {
        return WcAppInfoUM.Loading(
            onDismiss = ::dismiss,
            connectButtonConfig = WcPrimaryButtonConfig(showProgress = false, enabled = false, onClick = ::onConnect),
        )
    }
}
