package com.tangem.features.details.component.preview

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.tangem.features.details.component.WalletConnectComponent
import com.tangem.features.details.ui.WalletConnectBlock

internal class PreviewWalletConnectComponent : WalletConnectComponent {

    override suspend fun checkIsAvailable(): Boolean = true

    @Composable
    @Suppress("TopLevelComposableFunctions") // TODO: Remove this check
    override fun View(modifier: Modifier) {
        WalletConnectBlock(onClick = { /* no-op */ }, modifier = modifier)
    }
}