package com.tangem.features.walletconnect.transaction.components.chain

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.tangem.core.decompose.context.AppComponentContext
import com.tangem.core.decompose.model.getOrCreateModel
import com.tangem.core.ui.decompose.ComposableContentComponent
import com.tangem.features.walletconnect.transaction.components.common.WcTransactionModelParams
import com.tangem.features.walletconnect.transaction.model.WcSwitchNetworkModel

internal class WcSwitchNetworkComponent(
    appComponentContext: AppComponentContext,
    params: WcTransactionModelParams,
) : AppComponentContext by appComponentContext, ComposableContentComponent {

    private val model: WcSwitchNetworkModel = getOrCreateModel(params = params)

    @Composable
    override fun Content(modifier: Modifier) {
    }
}