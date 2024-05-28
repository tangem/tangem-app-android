package com.tangem.features.details.component.preview

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.tangem.features.details.component.WalletConnectComponent

internal class PreviewWalletConnectComponent : WalletConnectComponent {

    override suspend fun checkIsAvailable(): Boolean = true

    @Composable
    @Suppress("TopLevelComposableFunctions") // TODO: Remove this check
    override fun View(modifier: Modifier) {
        TODO("Will be implemented in AND-7107")
    }
}
