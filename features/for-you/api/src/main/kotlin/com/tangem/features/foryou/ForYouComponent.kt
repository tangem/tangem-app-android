package com.tangem.features.foryou

import com.tangem.core.decompose.factory.ComponentFactory
import com.tangem.core.ui.decompose.ComposableModularBottomSheetContentComponent
import com.tangem.domain.models.currency.CryptoCurrency
import com.tangem.domain.models.wallet.UserWalletId

interface ForYouComponent : ComposableModularBottomSheetContentComponent {

    data class Params(
        val callbacks: ForYouModelCallbacks,
    )

    interface ForYouModelCallbacks {
        fun onTokenClick(userWalletId: UserWalletId, currency: CryptoCurrency)
        fun onAllEarnTokensClick()
    }

    interface Factory : ComponentFactory<Params, ForYouComponent>
}