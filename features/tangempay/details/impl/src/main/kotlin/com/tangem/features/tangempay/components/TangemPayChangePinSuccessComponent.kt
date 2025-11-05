package com.tangem.features.tangempay.components

import androidx.activity.compose.BackHandler
import androidx.compose.runtime.Composable
import com.tangem.features.tangempay.navigation.TangemPayDetailsInnerRoute
import androidx.compose.ui.Modifier
import com.tangem.core.decompose.context.AppComponentContext
import com.tangem.core.ui.decompose.ComposableContentComponent
import com.tangem.features.tangempay.ui.TangemPayChangePinCodeSuccessScreen

internal class TangemPayChangePinSuccessComponent(
    private val appComponentContext: AppComponentContext,
) : AppComponentContext by appComponentContext, ComposableContentComponent {

    @Composable
    override fun Content(modifier: Modifier) {
        BackHandler(onBack = ::backToDetails)
        TangemPayChangePinCodeSuccessScreen(onClick = ::backToDetails)
    }

    private fun backToDetails() {
        router.popTo(route = TangemPayDetailsInnerRoute.Details)
    }
}