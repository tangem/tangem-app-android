package com.tangem.features.txhistory.joint.operation

import com.tangem.core.decompose.factory.ComponentFactory
import com.tangem.core.ui.decompose.ComposableBottomSheetComponent
import com.tangem.domain.models.currency.CryptoCurrency
import com.tangem.domain.models.wallet.UserWalletId

/** One sheet over two screens — the operation and its signers — so moving between them keeps a single sheet on screen */
internal interface JointOperationComponent : ComposableBottomSheetComponent {

    /** The sheet closes itself once the backend stops reporting [operationId] — an executed or cancelled operation */
    data class Params(
        val userWalletId: UserWalletId,
        val currency: CryptoCurrency,
        val operationId: String,
        val onDismiss: () -> Unit,
    )

    interface Factory : ComponentFactory<Params, JointOperationComponent>
}