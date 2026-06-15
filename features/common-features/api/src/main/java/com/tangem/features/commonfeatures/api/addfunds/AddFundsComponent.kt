package com.tangem.features.commonfeatures.api.addfunds

import com.tangem.core.decompose.factory.ComponentFactory
import com.tangem.core.ui.decompose.ComposableBottomSheetComponent
import com.tangem.domain.models.currency.CryptoCurrency
import com.tangem.domain.models.wallet.UserWalletId

interface AddFundsComponent : ComposableBottomSheetComponent {

    data class Params(
        val launchMode: LaunchMode,
        val onDismiss: () -> Unit,
    )

    sealed interface LaunchMode {
        data class ChooseToken(val userWalletId: UserWalletId) : LaunchMode

        data class TokenActionsOnly(
            val userWalletId: UserWalletId,
            val currency: CryptoCurrency,
        ) : LaunchMode

        data class FilteredByRawId(
            val rawCurrencyId: CryptoCurrency.RawID,
        ) : LaunchMode
    }

    interface Factory : ComponentFactory<Params, AddFundsComponent>
}