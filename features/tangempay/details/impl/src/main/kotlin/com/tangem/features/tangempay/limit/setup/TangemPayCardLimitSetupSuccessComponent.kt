package com.tangem.features.tangempay.limit.setup

import androidx.activity.compose.BackHandler
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.tangem.core.decompose.context.AppComponentContext
import com.tangem.core.ui.decompose.ComposableContentComponent
import com.tangem.features.tangempay.navigation.TangemPayDetailsInnerRoute

internal class TangemPayCardLimitSetupSuccessComponent(
    appComponentContext: AppComponentContext,
) : AppComponentContext by appComponentContext, ComposableContentComponent {

    @Composable
    override fun Content(modifier: Modifier) {
        BackHandler(onBack = ::backToDetails)
        TangemPayCardLimitSetupSuccessScreen(
            modifier = modifier,
            onDoneClick = ::backToDetails,
        )
    }

    private fun backToDetails() {
        router.popTo(route = TangemPayDetailsInnerRoute.Details)
    }
}