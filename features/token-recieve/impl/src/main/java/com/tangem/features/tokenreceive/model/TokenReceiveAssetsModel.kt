package com.tangem.features.tokenreceive.model

import androidx.compose.runtime.Stable
import com.tangem.core.decompose.di.ModelScoped
import com.tangem.core.decompose.model.Model
import com.tangem.core.decompose.model.ParamsContainer
import com.tangem.features.tokenreceive.component.TokenReceiveAssetsComponent
import com.tangem.features.tokenreceive.ui.state.ReceiveAssetsUM
import com.tangem.utils.coroutines.CoroutineDispatcherProvider
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import javax.inject.Inject

@Stable
@ModelScoped
internal class TokenReceiveAssetsModel @Inject constructor(
    paramsContainer: ParamsContainer,
    override val dispatchers: CoroutineDispatcherProvider,
) : Model() {

    private val params = paramsContainer.require<TokenReceiveAssetsComponent.TokenReceiveAssetsParams>()

    internal val state: StateFlow<ReceiveAssetsUM>
    field = MutableStateFlow<ReceiveAssetsUM>(
        ReceiveAssetsUM(
            onCopyClick = params.callback::onCopyClick,
            onOpenQrCodeClick = params.callback::onQrCodeClick,
            addresses = params.addresses,
            showMemoDisclaimer = params.showMemoDisclaimer,
            isEnsResultLoading = false,
            notificationConfigs = params.notificationConfigs,
            fullName = params.fullName,
        ),
    )
}