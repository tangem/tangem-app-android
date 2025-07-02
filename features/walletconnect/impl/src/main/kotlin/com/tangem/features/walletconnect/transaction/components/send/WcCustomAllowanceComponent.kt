package com.tangem.features.walletconnect.transaction.components.send

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.tangem.core.decompose.context.AppComponentContext
import com.tangem.core.ui.decompose.ComposableBottomSheetComponent
import com.tangem.features.walletconnect.transaction.entity.common.WcCommonTransactionModel
import com.tangem.features.walletconnect.transaction.entity.send.WcSendTransactionUM
import com.tangem.features.walletconnect.transaction.ui.approve.WcCustomAllowanceContent
import java.math.BigDecimal

internal class WcCustomAllowanceComponent(
    private val appComponentContext: AppComponentContext,
    private val model: WcCommonTransactionModel,
    private val onClickDoneCustomAllowance: (BigDecimal, Boolean) -> Unit,
) : AppComponentContext by appComponentContext, ComposableBottomSheetComponent {

    override fun dismiss() {
        model.dismiss()
    }

    @Composable
    override fun BottomSheet() {
        val content by model.uiState.collectAsStateWithLifecycle()
        val spendAllowance = (content as? WcSendTransactionUM)?.spendAllowance
        if (spendAllowance != null) {
            WcCustomAllowanceContent(
                state = spendAllowance,
                onClickDone = { value, isUnlimited ->
                    router.pop()
                    onClickDoneCustomAllowance(value, isUnlimited)
                },
                onBack = router::pop,
                onDismiss = ::dismiss,
            )
        }
    }
}