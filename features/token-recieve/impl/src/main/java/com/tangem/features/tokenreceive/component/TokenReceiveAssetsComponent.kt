package com.tangem.features.tokenreceive.component

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.tangem.common.ui.notifications.NotificationUM
import com.tangem.core.decompose.context.AppComponentContext
import com.tangem.core.decompose.model.getOrCreateModel
import com.tangem.core.ui.decompose.ComposableContentComponent
import com.tangem.domain.tokens.model.analytics.TokenReceiveCopyActionSource
import com.tangem.features.tokenreceive.entity.ReceiveAddress
import com.tangem.features.tokenreceive.model.TokenReceiveAssetsModel
import com.tangem.features.tokenreceive.ui.TokenReceiveAssetsContent
import kotlinx.collections.immutable.ImmutableList

internal class TokenReceiveAssetsComponent(
    appComponentContext: AppComponentContext,
    private val params: TokenReceiveAssetsParams,
) : AppComponentContext by appComponentContext, ComposableContentComponent {

    private val model: TokenReceiveAssetsModel = getOrCreateModel(params = params)

    @Composable
    override fun Content(modifier: Modifier) {
        val state by model.state.collectAsStateWithLifecycle()
        TokenReceiveAssetsContent(assetsUM = state)
    }

    internal interface TokenReceiveAssetsModelCallback {
        fun onQrCodeClick(address: String)
        fun onCopyClick(address: ReceiveAddress, source: TokenReceiveCopyActionSource)
    }

    data class TokenReceiveAssetsParams(
        val notificationConfigs: ImmutableList<NotificationUM>,
        val addresses: ImmutableList<ReceiveAddress>,
        val callback: TokenReceiveAssetsModelCallback,
        val onDismiss: () -> Unit,
        val showMemoDisclaimer: Boolean,
        val fullName: String,
        val tokenName: String,
    )
}