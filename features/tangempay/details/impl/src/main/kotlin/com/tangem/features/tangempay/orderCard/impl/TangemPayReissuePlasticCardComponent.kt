package com.tangem.features.tangempay.orderCard.impl

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.tangem.core.decompose.context.AppComponentContext
import com.tangem.core.decompose.model.getOrCreateModel
import com.tangem.core.ui.decompose.ComposableBottomSheetComponent
import com.tangem.domain.models.wallet.UserWalletId
import com.tangem.features.tangempay.orderCard.impl.model.TangemPayReissuePlasticCardModel
import com.tangem.features.tangempay.orderCard.impl.ui.TangemPayReissuePlasticCardContent

internal class TangemPayReissuePlasticCardComponent(
    appComponentContext: AppComponentContext,
    params: Params,
) : ComposableBottomSheetComponent, AppComponentContext by appComponentContext {

    private val model: TangemPayReissuePlasticCardModel = getOrCreateModel(params = params)

    override fun dismiss() = model.onDismiss()

    @Composable
    override fun BottomSheet() {
        val state by model.state.collectAsStateWithLifecycle()

        TangemPayReissuePlasticCardContent(state = state)
    }

    data class Params(
        val userWalletId: UserWalletId,
        val sourceProductInstanceId: String,
        val onDismiss: () -> Unit,
        val onReplaceConfirmed: (deliveryEtaMaxBusinessDays: Int) -> Unit,
    )
}