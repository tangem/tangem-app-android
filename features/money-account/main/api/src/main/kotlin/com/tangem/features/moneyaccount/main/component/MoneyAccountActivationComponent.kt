package com.tangem.features.moneyaccount.main.component

import com.tangem.core.decompose.factory.ComponentFactory
import com.tangem.core.ui.decompose.ComposableBottomSheetComponent
import com.tangem.domain.models.wallet.UserWalletId

interface MoneyAccountActivationComponent : ComposableBottomSheetComponent {

    data class Params(val userWalletId: UserWalletId, val onDismiss: () -> Unit)

    interface Factory : ComponentFactory<Params, MoneyAccountActivationComponent>
}