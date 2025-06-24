package com.tangem.features.swap.v2.api.choosetoken

import com.tangem.core.decompose.factory.ComponentFactory
import com.tangem.core.ui.decompose.ComposableBottomSheetComponent
import com.tangem.domain.managetokens.model.ManagedCryptoCurrency
import com.tangem.domain.models.currency.CryptoCurrency
import com.tangem.domain.wallets.models.UserWalletId

interface SwapChooseTokenNetworkComponent : ComposableBottomSheetComponent {

    data class Params(
        val userWalletId: UserWalletId,
        val initialCurrency: CryptoCurrency,
        val token: ManagedCryptoCurrency.Token,
        val onDismiss: () -> Unit,
        val onResult: (CryptoCurrency) -> Unit,
    )

    interface Factory : ComponentFactory<Params, SwapChooseTokenNetworkComponent>
}