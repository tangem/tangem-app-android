package com.tangem.features.tangempay.card.gpay

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

internal class TangemPayAddToWalletComponent(
    private val appComponentContext: AppComponentContext,
    params: Params,
) : AppComponentContext by appComponentContext, ComposableContentComponent {

    private val model: TangemPayAddToWalletModel = getOrCreateModel(params)

    @Composable
    override fun Content(modifier: Modifier) {
        val state by model.uiState.collectAsStateWithLifecycle()
        val cardDetailsState by model.cardDetailsState.collectAsStateWithLifecycle()
        BackHandler(onBack = router::pop)
        TangemPayAddToWalletScreen(
            state = state,
            cardDetailsState = cardDetailsState,
        )
    }

    data class Params(val card: TangemPayCard, val userWalletId: UserWalletId)
}