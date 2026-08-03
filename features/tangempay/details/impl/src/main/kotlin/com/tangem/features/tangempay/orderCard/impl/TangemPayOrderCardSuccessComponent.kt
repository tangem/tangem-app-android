package com.tangem.features.tangempay.orderCard.impl

import androidx.activity.compose.BackHandler
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.tangem.core.decompose.context.AppComponentContext
import com.tangem.core.ui.decompose.ComposableContentComponent
import com.tangem.features.tangempay.orderCard.impl.ui.TangemPayOrderCardSuccessScreen

internal class TangemPayOrderCardSuccessComponent(
    appComponentContext: AppComponentContext,
    private val params: Params,
) : ComposableContentComponent, AppComponentContext by appComponentContext {

    @Composable
    override fun Content(modifier: Modifier) {
        BackHandler(onBack = params.onDone)
        TangemPayOrderCardSuccessScreen(onDone = params.onDone, modifier = modifier)
    }

    data class Params(val onDone: () -> Unit)
}