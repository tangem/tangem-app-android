package com.tangem.features.tangempay.components

import com.tangem.core.decompose.factory.ComponentFactory
import com.tangem.core.ui.decompose.ComposableBottomSheetComponent
import com.tangem.domain.models.wallet.UserWalletId
import com.tangem.domain.visa.model.TangemPayTxHistoryItem

interface TangemPayTransactionBottomSheetComponent : ComposableBottomSheetComponent {

    data class Params(
        val isBalanceHidden: Boolean,
        val transaction: TangemPayTxHistoryItem,
        val userWalletId: UserWalletId,
        val customerId: String,
        val onDismiss: () -> Unit,
    )

    interface Factory : ComponentFactory<Params, TangemPayTransactionBottomSheetComponent>
}