package com.tangem.features.swap.v2.impl.sendviaswap.success

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.tangem.common.ui.navigationButtons.NavigationModelCallback
import com.tangem.core.decompose.context.AppComponentContext
import com.tangem.core.decompose.model.getOrCreateModel
import com.tangem.core.ui.decompose.ComposableContentComponent
import com.tangem.features.swap.v2.impl.sendviaswap.SendWithSwapRoute
import com.tangem.features.swap.v2.impl.sendviaswap.entity.SendWithSwapUM
import com.tangem.features.swap.v2.impl.sendviaswap.success.model.SendWithSwapSuccessModel
import com.tangem.features.swap.v2.impl.sendviaswap.success.ui.SendWithSwapSuccessContent
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow

internal class SendWithSwapSuccessComponent(
    appComponentContext: AppComponentContext,
    params: Params,
) : ComposableContentComponent, AppComponentContext by appComponentContext {

    private val model: SendWithSwapSuccessModel = getOrCreateModel(params = params)

    @Composable
    override fun Content(modifier: Modifier) {
        val state by model.uiState.collectAsStateWithLifecycle()
        SendWithSwapSuccessContent(sendWithSwapUM = state)
    }

    data class Params(
        val sendWithSwapUMFlow: StateFlow<SendWithSwapUM>,
        val analyticsCategoryName: String,
        val currentRoute: Flow<SendWithSwapRoute.Success>,
        val callback: NavigationModelCallback,
    )
}