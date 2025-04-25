package com.tangem.features.walletconnect.connections.components

import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.tangem.core.decompose.context.AppComponentContext
import com.tangem.core.ui.decompose.ComposableContentComponent
import com.tangem.core.ui.res.TangemTheme
import com.tangem.features.walletconnect.connections.model.WcAppInfoModel
import com.tangem.features.walletconnect.connections.ui.WcAppInfoModalBottomSheet

internal class WcAppInfoComponent(
    private val appComponentContext: AppComponentContext,
    private val model: WcAppInfoModel,
) : AppComponentContext by appComponentContext, ComposableContentComponent {

    @Composable
    override fun Content(modifier: Modifier) {
        val state by model.appInfoUiState.collectAsStateWithLifecycle()
        WcAppInfoModalBottomSheet(
            modifier = modifier.padding(horizontal = TangemTheme.dimens.spacing16),
            state = state,
        )
    }
}