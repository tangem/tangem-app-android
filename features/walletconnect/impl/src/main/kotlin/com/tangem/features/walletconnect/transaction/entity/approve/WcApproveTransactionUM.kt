package com.tangem.features.walletconnect.transaction.entity.approve

import androidx.compose.runtime.Immutable
import com.tangem.core.ui.components.bottomsheets.TangemBottomSheetConfigContent
import com.tangem.features.walletconnect.transaction.entity.common.WcTransactionActionsUM
import com.tangem.features.walletconnect.transaction.entity.common.WcTransactionRequestInfoUM
import com.tangem.features.walletconnect.transaction.entity.common.WcTransactionUM

@Immutable
internal data class WcApproveTransactionUM(
    val actions: WcTransactionActionsUM,
    val state: State = State.TRANSACTION,
    val transaction: WcTransactionUM,
    val transactionRequestInfo: WcTransactionRequestInfoUM,
) : TangemBottomSheetConfigContent {

    enum class State {
        TRANSACTION, CUSTOM_ALLOWANCE, TRANSACTION_REQUEST_INFO
    }
}