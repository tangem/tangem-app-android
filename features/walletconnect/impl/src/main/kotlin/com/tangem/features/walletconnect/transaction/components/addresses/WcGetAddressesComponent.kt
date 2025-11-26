package com.tangem.features.walletconnect.transaction.components.addresses

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.tangem.core.decompose.context.AppComponentContext
import com.tangem.core.decompose.model.getOrCreateModel
import com.tangem.core.ui.decompose.ComposableContentComponent
import com.tangem.core.ui.res.TangemTheme
import com.tangem.features.walletconnect.transaction.components.common.WcTransactionModelParams
import com.tangem.features.walletconnect.transaction.model.WcGetAddressesModel
import com.tangem.features.walletconnect.transaction.ui.addresses.WcGetAddressesContent

/**
 * Component for Bitcoin getAccountAddresses WalletConnect method.
 *
 * Shows a confirmation dialog to the user before sharing wallet addresses.
 */
internal class WcGetAddressesComponent(
    appComponentContext: AppComponentContext,
    params: WcTransactionModelParams,
) : AppComponentContext by appComponentContext, ComposableContentComponent {

    private val model: WcGetAddressesModel = getOrCreateModel(params = params)

    @Composable
    override fun Content(modifier: Modifier) {
        val state = model.uiState

        if (state != null) {
            WcGetAddressesContent(
                state = state,
                onDismiss = { router.pop() },
                modifier = modifier,
            )
        } else {
            // Loading state while use case is being created
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center,
            ) {
                CircularProgressIndicator(
                    color = TangemTheme.colors.icon.primary1,
                )
            }
        }
    }
}