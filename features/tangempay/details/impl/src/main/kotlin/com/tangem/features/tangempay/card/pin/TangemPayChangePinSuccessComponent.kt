package com.tangem.features.tangempay.card.pin

import androidx.activity.compose.BackHandler
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.tangem.core.decompose.context.AppComponentContext
import com.tangem.core.ui.decompose.ComposableContentComponent
import com.tangem.features.tangempay.card.details.TangemPayCardDetailsInnerRoute

internal class TangemPayChangePinSuccessComponent(
    private val appComponentContext: AppComponentContext,
) : AppComponentContext by appComponentContext, ComposableContentComponent {

    @Composable
    override fun Content(modifier: Modifier) {
        BackHandler(onBack = ::backToDetails)
        TangemPayChangePinCodeSuccessScreen(onClose = ::backToDetails)
    }

    private fun backToDetails() {
        router.popTo(route = TangemPayCardDetailsInnerRoute.Details)
    }
}