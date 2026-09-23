package com.tangem.features.tangempay.eligibility

import com.tangem.core.decompose.factory.ComponentFactory
import com.tangem.core.ui.decompose.ComposableBottomSheetComponent
import com.tangem.domain.models.wallet.UserWalletId

interface TangemPayWalletSelectorComponent : ComposableBottomSheetComponent {

    data class Params(
        val listener: Listener,
        val walletsIds: List<UserWalletId>,
    )

    interface Listener {
        fun onWalletSelected(userWalletId: UserWalletId)
        fun onWalletSelectorDismiss()
    }

    interface Factory : ComponentFactory<Params, TangemPayWalletSelectorComponent>
}