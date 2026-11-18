package com.tangem.features.tokenreceive.component

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.tangem.common.ui.notifications.NotificationUM
import com.tangem.core.decompose.context.AppComponentContext
import com.tangem.core.decompose.model.getOrCreateModel
import com.tangem.core.ui.decompose.ComposableContentComponent
import com.tangem.domain.models.TokenReceiveConfig
import com.tangem.domain.tokens.model.analytics.TokenReceiveCopyActionSource
import com.tangem.features.tokenreceive.entity.ReceiveAddress
import com.tangem.features.tokenreceive.model.TokenReceiveScreenModel
import com.tangem.features.tokenreceive.ui.TokenReceiveScreenContent
import com.tangem.features.tokenreceive.ui.state.TokenReceiveUM
import kotlinx.collections.immutable.ImmutableList
import kotlinx.coroutines.flow.StateFlow

internal class TokenReceiveScreenComponent(
    appComponentContext: AppComponentContext,
    params: Params,
) : AppComponentContext by appComponentContext, ComposableContentComponent {

    private val model: TokenReceiveScreenModel = getOrCreateModel(params = params)

    fun onShareClick() = model.onShareClick()

    @Composable
    override fun Content(modifier: Modifier) {
        val state by model.state.collectAsStateWithLifecycle()
        TokenReceiveScreenContent(state = state, modifier = modifier)
    }

    internal interface ModelCallback {
        fun onCopyClick(address: ReceiveAddress, source: TokenReceiveCopyActionSource)
        fun onShareClick(address: String)
    }

    data class Params(
        val config: TokenReceiveConfig,
        val tokenName: String,
        val receiveState: StateFlow<TokenReceiveUM>,
        val warnings: ImmutableList<NotificationUM>,
        val callback: ModelCallback,
    )
}