package com.tangem.features.tokenreceive.model

import androidx.compose.runtime.Stable
import com.tangem.core.decompose.di.ModelScoped
import com.tangem.core.decompose.model.Model
import com.tangem.core.decompose.model.ParamsContainer
import com.tangem.core.ui.extensions.TextReference
import com.tangem.domain.tokens.model.analytics.TokenReceiveCopyActionSource
import com.tangem.features.tokenreceive.component.TokenReceiveQrCodeComponent
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
                network = params.cryptoCurrency.network.name,
                addressValue = params.address.value,
                addressName = TextReference.Str("${params.cryptoCurrency.name} (${params.cryptoCurrency.symbol})"),
                onCopyClick = {
                    params.callback.onCopyClick(
                        address = params.address,
                        source = TokenReceiveCopyActionSource.QR,
                    )
                },
                onShareClick = params.callback::onShareClick,
            ),
        )
}