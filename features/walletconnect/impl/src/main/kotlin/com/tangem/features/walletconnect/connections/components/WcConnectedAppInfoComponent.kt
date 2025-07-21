package com.tangem.features.walletconnect.connections.components

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.tangem.core.decompose.context.AppComponentContext
import com.tangem.core.ui.decompose.ComposableBottomSheetComponent
import com.tangem.features.walletconnect.connections.model.WcConnectedAppInfoModel
import com.tangem.features.walletconnect.connections.ui.WcConnectedAppInfoBS

internal class WcConnectedAppInfoComponent(
    appComponentContext: AppComponentContext,
    private val model: WcConnectedAppInfoModel,
) : AppComponentContext by appComponentContext, ComposableBottomSheetComponent {

    override fun dismiss() {
        model.dismiss()
    }

    @Composable
    override fun BottomSheet() {
        val state by model.uiState.collectAsStateWithLifecycle()
        val appInfoUM = state
        if (appInfoUM != null) {
            WcConnectedAppInfoBS(appInfoUM)
        }
    }
}