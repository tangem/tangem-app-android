package com.tangem.features.tokenreceive.component

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.tangem.core.decompose.context.AppComponentContext
import com.tangem.core.decompose.model.getOrCreateModel
import com.tangem.core.ui.decompose.ComposableContentComponent
import com.tangem.domain.models.currency.CryptoCurrency
import com.tangem.domain.tokens.model.analytics.TokenReceiveCopyActionSource
import com.tangem.features.tokenreceive.entity.ReceiveAddress
import com.tangem.features.tokenreceive.model.TokenReceiveQrCodeModel
import com.tangem.features.tokenreceive.ui.TokenReceiveQrCodeContent

internal class TokenReceiveQrCodeComponent(
    appComponentContext: AppComponentContext,
    private val params: TokenReceiveQrCodeParams,
) : AppComponentContext by appComponentContext, ComposableContentComponent {

    private val model: TokenReceiveQrCodeModel = getOrCreateModel(params = params)

    @Composable
    override fun Content(modifier: Modifier) {
        val state by model.state.collectAsStateWithLifecycle()
        TokenReceiveQrCodeContent(qrCodeUM = state)
    }

    internal interface TokenReceiveQrCodeModelCallback {
        fun onCopyClick(address: ReceiveAddress, source: TokenReceiveCopyActionSource)
        fun onShareClick(address: String)
    }

    data class TokenReceiveQrCodeParams(
        val cryptoCurrency: CryptoCurrency,
        val address: ReceiveAddress,
        val callback: TokenReceiveQrCodeModelCallback,
        val onDismiss: () -> Unit,
    )
}