package com.tangem.features.walletconnect.connections.model.transformers

import com.tangem.features.walletconnect.connections.entity.WcAppInfoUM
import com.tangem.utils.transformer.Transformer

internal class WcConnectButtonProgressTransformer(private val showProgress: Boolean) : Transformer<WcAppInfoUM> {
    override fun transform(prevState: WcAppInfoUM): WcAppInfoUM {
        val contentState = prevState as? WcAppInfoUM.Content ?: return prevState
        return contentState.copy(
            connectButtonConfig = contentState.connectButtonConfig.copy(showProgress = showProgress),
        )
    }
}