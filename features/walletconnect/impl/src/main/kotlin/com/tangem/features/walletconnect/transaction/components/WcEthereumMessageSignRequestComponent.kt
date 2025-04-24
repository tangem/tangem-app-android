package com.tangem.features.walletconnect.transaction.components

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.tangem.core.decompose.context.AppComponentContext
import com.tangem.core.decompose.model.getOrCreateModel
import com.tangem.core.ui.components.bottomsheets.TangemBottomSheetConfig
import com.tangem.core.ui.decompose.ComposableContentComponent
import com.tangem.domain.walletconnect.model.sdkcopy.WcSdkSessionRequest
import com.tangem.features.walletconnect.transaction.model.WcEthereumMessageSignRequestModel
import com.tangem.features.walletconnect.transaction.ui.sign.WcEthereumMessageSignRequestModalBottomSheet

internal class WcEthereumMessageSignRequestComponent(
    appComponentContext: AppComponentContext,
    private val params: Params,
) : AppComponentContext by appComponentContext, ComposableContentComponent {

    private val model: WcEthereumMessageSignRequestModel = getOrCreateModel(params)

    @Composable
    override fun Content(modifier: Modifier) {
        val content by model.uiState.collectAsStateWithLifecycle()
        content?.let {
            WcEthereumMessageSignRequestModalBottomSheet(
                config = TangemBottomSheetConfig(
                    isShown = true,
                    onDismissRequest = {},
                    content = it,
                ),
            )
        }
    }

    data class Params(val rawRequest: WcSdkSessionRequest)
}