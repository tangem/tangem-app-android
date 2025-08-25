package com.tangem.features.tokenreceive.model

import androidx.compose.runtime.Stable
import com.tangem.core.decompose.di.ModelScoped
import com.tangem.core.decompose.model.Model
import com.tangem.core.decompose.model.ParamsContainer
import com.tangem.core.ui.extensions.TextReference
import com.tangem.features.tokenreceive.component.TokenReceiveQrCodeComponent
import com.tangem.features.tokenreceive.entity.ReceiveAddress
import com.tangem.features.tokenreceive.ui.state.QrCodeUM
import com.tangem.utils.coroutines.CoroutineDispatcherProvider
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import javax.inject.Inject

@Stable
@ModelScoped
internal class TokenReceiveQrCodeModel @Inject constructor(
    paramsContainer: ParamsContainer,
    override val dispatchers: CoroutineDispatcherProvider,
) : Model() {

    private val params = paramsContainer.require<TokenReceiveQrCodeComponent.TokenReceiveQrCodeParams>()

    internal val state: StateFlow<QrCodeUM>
    field = MutableStateFlow<QrCodeUM>(
        QrCodeUM(
            network = params.network,
            addressValue = params.address.value,
            addressName = (params.address.type as? ReceiveAddress.Type.Default)?.displayName ?: TextReference.EMPTY,
            onCopyClick = { params.callback.onCopyClick(params.id) },
            onShareClick = params.callback::onShareClick,
        ),
    )
}