package com.tangem.features.walletconnect.connections.components

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.tangem.core.decompose.context.AppComponentContext
import com.tangem.core.ui.decompose.ComposableBottomSheetComponent
import com.tangem.features.walletconnect.connections.model.WcPairModel
import com.tangem.features.walletconnect.connections.ui.WcAppInfoModalBottomSheet

internal class WcAppInfoComponent(
    private val appComponentContext: AppComponentContext,
    private val model: WcPairModel,
    private val onDismiss: () -> Unit,
) : AppComponentContext by appComponentContext, ComposableBottomSheetComponent {

    override fun dismiss() {
        onDismiss()
    }

    @Composable
    override fun BottomSheet() {
        val state by model.appInfoUiState.collectAsStateWithLifecycle()

        WcAppInfoModalBottomSheet(state = state, onBack = router::pop, onDismiss = ::dismiss)
    }
}