package com.tangem.features.tangempay.withdraw.bank

import androidx.activity.compose.BackHandler
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.tangem.core.decompose.context.AppComponentContext
import com.tangem.core.ui.decompose.ComposableContentComponent
import com.tangem.domain.models.wallet.UserWalletId
import com.tangem.features.tangempay.withdraw.bank.ui.TangemPayBankWithdrawScreen
import com.tangem.features.tangempay.withdraw.bank.ui.toScreenTitle

internal class TangemPayBankWithdrawComponent(
    appComponentContext: AppComponentContext,
    private val params: Params,
) : ComposableContentComponent, AppComponentContext by appComponentContext {

    @Composable
    override fun Content(modifier: Modifier) {
        BackHandler(onBack = router::pop)

        TangemPayBankWithdrawScreen(
            title = params.entry.toScreenTitle(),
            onBackClick = router::pop,
            modifier = modifier,
        )
    }

    data class Params(val userWalletId: UserWalletId, val entry: BankWithdrawEntry)
}