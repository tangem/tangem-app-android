package com.tangem.features.swap.v2.api.choosetoken

import com.tangem.core.decompose.factory.ComponentFactory
import com.tangem.core.ui.decompose.ComposableBottomSheetComponent
import com.tangem.domain.managetokens.model.ManagedCryptoCurrency
import com.tangem.domain.models.currency.CryptoCurrency
import com.tangem.domain.models.wallet.UserWalletId
import com.tangem.domain.swap.models.SwapCurrencies

interface SwapChooseTokenNetworkComponent : ComposableBottomSheetComponent {

    data class Params(
        val userWalletId: UserWalletId,
        val initialCurrency: CryptoCurrency,
        val analyticsCategoryName: String,
        val selectedCurrency: CryptoCurrency?,
        val token: ManagedCryptoCurrency.Token,
        val isSearchedToken: Boolean,
        val onDismiss: () -> Unit,
        val onResult: (SwapCurrencies, CryptoCurrency) -> Unit,
    )

    interface Factory : ComponentFactory<Params, SwapChooseTokenNetworkComponent>
}