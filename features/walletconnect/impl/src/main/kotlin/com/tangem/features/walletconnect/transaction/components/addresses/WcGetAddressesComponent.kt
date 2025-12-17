package com.tangem.features.walletconnect.transaction.components.addresses

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.tangem.core.decompose.context.AppComponentContext
import com.tangem.core.decompose.model.getOrCreateModel
import com.tangem.core.ui.decompose.ComposableContentComponent
import com.tangem.features.walletconnect.transaction.components.common.WcTransactionModelParams
import com.tangem.features.walletconnect.transaction.model.WcGetAddressesModel

/**
 * Component for Bitcoin getAccountAddresses WalletConnect method.
 */
internal class WcGetAddressesComponent(
    appComponentContext: AppComponentContext,
    params: WcTransactionModelParams,
) : AppComponentContext by appComponentContext, ComposableContentComponent {

    @Suppress("UnusedPrivateProperty")
    private val model: WcGetAddressesModel = getOrCreateModel(params = params)

    @Composable
    override fun Content(modifier: Modifier) {
    }
}