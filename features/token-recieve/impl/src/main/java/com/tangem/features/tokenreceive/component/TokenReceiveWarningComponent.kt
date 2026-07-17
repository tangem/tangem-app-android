package com.tangem.features.tokenreceive.component

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.tangem.core.decompose.context.AppComponentContext
import com.tangem.core.decompose.model.getOrCreateModel
import com.tangem.core.ui.components.currency.icon.CurrencyIconState
import com.tangem.core.ui.decompose.ComposableContentComponent
import com.tangem.domain.models.network.Network
import com.tangem.features.tokenreceive.model.TokenReceiveWarningModel
import com.tangem.features.tokenreceive.ui.TokenReceiveWarningContent

internal class TokenReceiveWarningComponent(
    appComponentContext: AppComponentContext,
    private val params: TokenReceiveWarningParams,
) : AppComponentContext by appComponentContext, ComposableContentComponent {

    private val model: TokenReceiveWarningModel = getOrCreateModel(params = params)

    @Composable
    override fun Content(modifier: Modifier) {
        val state by model.state.collectAsStateWithLifecycle()
        TokenReceiveWarningContent(warningUM = state)
    }

    internal interface TokenReceiveWarningModelCallback {
        fun onWarningAcknowledged()
    }

    data class TokenReceiveWarningParams(
        val iconState: CurrencyIconState,
        val callback: TokenReceiveWarningModelCallback,
        val onDismiss: () -> Unit,
        val network: Network,
    )
}