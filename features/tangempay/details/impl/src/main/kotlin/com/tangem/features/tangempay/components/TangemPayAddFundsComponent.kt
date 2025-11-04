package com.tangem.features.tangempay.components

import androidx.compose.runtime.Composable
import com.tangem.core.decompose.context.AppComponentContext
import com.tangem.core.decompose.model.getOrCreateModel
import com.tangem.core.ui.decompose.ComposableBottomSheetComponent
import com.tangem.domain.models.wallet.UserWalletId
import com.tangem.domain.pay.TangemPayTopUpData
import com.tangem.features.tangempay.model.TangemPayAddFundsModel
import com.tangem.features.tangempay.ui.TangemPayAddFundsContent
import java.math.BigDecimal

internal class TangemPayAddFundsComponent(
    appComponentContext: AppComponentContext,
    params: Params,
) : ComposableBottomSheetComponent, AppComponentContext by appComponentContext {

    private val model: TangemPayAddFundsModel = getOrCreateModel(params = params)

    override fun dismiss() {
        model.onDismiss()
    }

    @Composable
    override fun BottomSheet() {
        TangemPayAddFundsContent(state = model.uiState)
    }

    data class Params(
        val listener: AddFundsListener,
        val walletId: UserWalletId,
        val cryptoBalance: BigDecimal,
        val fiatBalance: BigDecimal,
        val depositAddress: String,
        val chainId: Int,
    )
}

internal interface AddFundsListener {
    fun onClickReceive(data: TangemPayTopUpData)
    fun onClickSwap(data: TangemPayTopUpData)
    fun onDismissAddFunds()
}