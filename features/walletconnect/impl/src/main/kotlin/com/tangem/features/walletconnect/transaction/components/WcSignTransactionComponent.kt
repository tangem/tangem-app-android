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
import com.tangem.features.walletconnect.transaction.model.WcSignTransactionModel
import com.tangem.features.walletconnect.transaction.ui.sign.WcSignTransactionModalBottomSheet

internal class WcSignTransactionComponent(
    appComponentContext: AppComponentContext,
    params: Params,
) : AppComponentContext by appComponentContext, ComposableContentComponent {

    private val model: WcSignTransactionModel = getOrCreateModel(params)

    @Composable
    override fun Content(modifier: Modifier) {
        val content by model.uiState.collectAsStateWithLifecycle()
        content?.let {
            WcSignTransactionModalBottomSheet(
                config = TangemBottomSheetConfig(
                    isShown = true,
                    onDismissRequest = it.actions.onDismiss,
                    content = it,
                ),
            )
        }
    }

    data class Params(val rawRequest: WcSdkSessionRequest)
}