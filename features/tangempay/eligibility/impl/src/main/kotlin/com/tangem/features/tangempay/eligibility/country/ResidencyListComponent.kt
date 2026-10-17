package com.tangem.features.tangempay.eligibility.country

import androidx.activity.compose.BackHandler
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.tangem.core.decompose.context.AppComponentContext
import com.tangem.core.decompose.model.getOrCreateModel
import com.tangem.core.ui.decompose.ComposableContentComponent
import com.tangem.domain.models.pay.TangemPayProduct
import com.tangem.domain.models.wallet.UserWalletId

internal class ResidencyListComponent(
    appComponentContext: AppComponentContext,
    params: Params,
) : ComposableContentComponent, AppComponentContext by appComponentContext {

    private val model: ResidencyListModel = getOrCreateModel(params)

    @Composable
    override fun Content(modifier: Modifier) {
        val state by model.uiState.collectAsStateWithLifecycle()

        ResidencyListScreen(state = state, modifier = modifier)

        BackHandler(onBack = state.onBackClick ?: state.onCloseClick)
    }

    data class Params(
        val product: TangemPayProduct,
        val userWalletId: UserWalletId,
        val step: Step,
        val onCloseClick: () -> Unit,
    )

    sealed interface Step {
        data object Countries : Step
        data class Regions(val countryCode: String, val countryName: String) : Step
    }
}