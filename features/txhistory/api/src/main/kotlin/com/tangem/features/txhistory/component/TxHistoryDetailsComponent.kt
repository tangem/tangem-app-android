package com.tangem.features.txhistory.component

import com.tangem.core.decompose.factory.ComponentFactory
import com.tangem.core.ui.decompose.ComposableBottomSheetComponent
import com.tangem.domain.models.currency.CryptoCurrency
import com.tangem.domain.models.network.TxInfo
import com.tangem.domain.models.wallet.UserWalletId
import kotlinx.coroutines.flow.Flow

interface TxHistoryDetailsComponent : ComposableBottomSheetComponent {

    data class Params(
        val txInfo: Flow<TxInfo>,
        val userWalletId: UserWalletId,
        val currency: CryptoCurrency,
        val onDismiss: () -> Unit,
    )

    interface Factory : ComponentFactory<Params, TxHistoryDetailsComponent>
}