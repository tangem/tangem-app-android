package com.tangem.features.txhistory.component

import com.tangem.core.decompose.factory.ComponentFactory
import com.tangem.core.ui.decompose.ComposableBottomSheetComponent
import com.tangem.domain.models.currency.CryptoCurrency
import com.tangem.domain.models.wallet.UserWalletId
import com.tangem.domain.txhistory.model.TxHistoryInfo
import kotlinx.coroutines.flow.Flow

interface TxHistoryDetailsComponent : ComposableBottomSheetComponent {

    data class Params(
        val txHistoryInfo: Flow<TxHistoryInfo>,
        val userWalletId: UserWalletId,
        val currency: CryptoCurrency,
        val onDismiss: () -> Unit,
    )

    interface Factory : ComponentFactory<Params, TxHistoryDetailsComponent>
}