package com.tangem.features.managetokens.component

import com.tangem.core.decompose.factory.ComponentFactory
import com.tangem.core.ui.decompose.ComposableBottomSheetComponent
import com.tangem.domain.models.wallet.UserWalletId

interface AddCustomTokenComponent : ComposableBottomSheetComponent {

    data class Params(
        val userWalletId: UserWalletId,
        val source: ManageTokensSource,
        val onDismiss: () -> Unit,
        val onCurrencyAdded: () -> Unit,
    )

    interface Factory : ComponentFactory<Params, AddCustomTokenComponent>
}