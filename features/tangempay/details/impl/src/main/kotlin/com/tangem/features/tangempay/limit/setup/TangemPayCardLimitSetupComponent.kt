package com.tangem.features.tangempay.limit.setup

import androidx.activity.compose.BackHandler
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.tangem.core.decompose.context.AppComponentContext
import com.tangem.core.decompose.model.getOrCreateModel
import com.tangem.core.ui.decompose.ComposableContentComponent
import com.tangem.domain.models.pay.TangemPayCard
import com.tangem.domain.models.wallet.UserWalletId

internal class TangemPayCardLimitSetupComponent(
    appComponentContext: AppComponentContext,
    params: Params,
) : AppComponentContext by appComponentContext, ComposableContentComponent {

    private val model: TangemPayCardLimitSetupModel = getOrCreateModel(params)

    @Composable
    override fun Content(modifier: Modifier) {
        val state by model.uiState.collectAsStateWithLifecycle()
        BackHandler(onBack = router::pop)
        if (model.inRedesignEnabled()) {
            TangemPayCardLimitSetupScreenV2(
                state = state,
                modifier = modifier,
            )
        } else {
            TangemPayCardLimitSetupScreen(
                state = state,
                modifier = modifier,
            )
        }
    }

    data class Params(val card: TangemPayCard, val userWalletId: UserWalletId)
}