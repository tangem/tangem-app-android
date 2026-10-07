package com.tangem.features.tangempay.addfunds.bankTransferOnramp.onboarding

import androidx.activity.compose.BackHandler
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.tangem.core.decompose.context.AppComponentContext
import com.tangem.core.decompose.model.getOrCreateModel
import com.tangem.core.ui.decompose.ComposableContentComponent
import com.tangem.domain.models.wallet.UserWalletId

internal class TangemPayBankTransferOnrampOnboardingComponent(
    appComponentContext: AppComponentContext,
    params: Params,
) : ComposableContentComponent, AppComponentContext by appComponentContext {

    private val model: TangemPayBankTransferOnrampOnboardingModel = getOrCreateModel(params = params)

    @Composable
    override fun Content(modifier: Modifier) {
        BackHandler(onBack = model::onCloseClick)
        val state by model.uiState.collectAsStateWithLifecycle()
        TangemPayBankTransferOnrampOnboardingScreen(state = state, modifier = modifier)
    }

    data class Params(val userWalletId: UserWalletId)
}