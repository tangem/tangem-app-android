package com.tangem.features.jointaccount.main.confirmaddress

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.tangem.core.decompose.context.AppComponentContext
import com.tangem.core.decompose.model.getOrCreateModel
import com.tangem.core.ui.decompose.ComposableContentComponent
import com.tangem.domain.models.wallet.UserWalletId
import com.tangem.features.jointaccount.main.confirmaddress.model.JointAccountConfirmAddressModel
import com.tangem.features.jointaccount.main.confirmaddress.ui.JointAccountConfirmAddressScreen

internal class JointAccountConfirmAddressComponent(
    appComponentContext: AppComponentContext,
    params: Params,
) : ComposableContentComponent, AppComponentContext by appComponentContext {

    private val model: JointAccountConfirmAddressModel = getOrCreateModel(params = params)

    @Composable
    override fun Content(modifier: Modifier) {
        val state by model.uiState.collectAsStateWithLifecycle()

        JointAccountConfirmAddressScreen(state = state, modifier = modifier)
    }

    data class Params(
        val userWalletId: UserWalletId,
        val cryptoAccountId: String,
    )
}