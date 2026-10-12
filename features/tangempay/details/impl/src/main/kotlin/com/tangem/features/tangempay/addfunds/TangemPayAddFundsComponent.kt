package com.tangem.features.tangempay.addfunds

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.tangem.core.decompose.context.AppComponentContext
import com.tangem.core.decompose.model.getOrCreateModel
import com.tangem.core.ui.decompose.ComposableBottomSheetComponent
import com.tangem.domain.models.account.VirtualAccountOnramp
import com.tangem.domain.models.currency.CryptoCurrency
import com.tangem.domain.models.wallet.UserWalletId
import com.tangem.domain.pay.model.TangemPayTopUpData
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
        val state by model.uiState.collectAsStateWithLifecycle()
        TangemPayAddFundsContent(state = state)
    }

    data class Params(
        val listener: AddFundsListener,
        val walletId: UserWalletId,
        val cryptoBalance: BigDecimal,
        val fiatBalance: BigDecimal,
        val depositAddress: String,
        val cryptoCurrency: CryptoCurrency,
        val virtualAccountOnramp: VirtualAccountOnramp?,
    )
}

internal interface AddFundsListener {
    fun onClickReceive(data: TangemPayTopUpData)
    fun onClickSwap(data: TangemPayTopUpData)
    fun onClickBankTransfer()
    fun onBankingDetailsLoadFailed(productInstanceId: String)
    fun onDismissAddFunds()
}