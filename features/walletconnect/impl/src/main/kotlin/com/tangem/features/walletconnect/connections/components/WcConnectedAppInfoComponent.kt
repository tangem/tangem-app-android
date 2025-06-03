package com.tangem.features.walletconnect.connections.components

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.tangem.core.decompose.context.AppComponentContext
import com.tangem.core.decompose.model.getOrCreateModel
import com.tangem.core.ui.decompose.ComposableBottomSheetComponent
import com.tangem.features.walletconnect.connections.model.WcConnectedAppInfoModel
import com.tangem.features.walletconnect.connections.ui.WcConnectedAppInfoBS

internal class WcConnectedAppInfoComponent(
    params: Params,
    appComponentContext: AppComponentContext,
) : AppComponentContext by appComponentContext, ComposableBottomSheetComponent {

    private val model: WcConnectedAppInfoModel = getOrCreateModel(params = params)

    override fun dismiss() {
        model.dismiss()
    }

    @Composable
    override fun BottomSheet() {
        val state by model.uiState.collectAsStateWithLifecycle()
        state?.let { WcConnectedAppInfoBS(it) }
    }

    data class Params(val topic: String, val onDismiss: () -> Unit)
}