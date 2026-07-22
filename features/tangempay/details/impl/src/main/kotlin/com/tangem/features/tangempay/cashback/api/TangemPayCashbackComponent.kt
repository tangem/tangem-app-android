package com.tangem.features.tangempay.cashback.api

import com.tangem.core.decompose.factory.ComponentFactory
import com.tangem.core.ui.decompose.ComposableContentComponent
import com.tangem.domain.models.wallet.UserWalletId

/**
 * Public contract of the Cashback screen. Kept in an `api` sub-package (separate from `impl`) so it
 * can be lifted into a dedicated module without refactoring.
 */
internal interface TangemPayCashbackComponent : ComposableContentComponent {

    data class Params(val userWalletId: UserWalletId)

    interface Factory : ComponentFactory<Params, TangemPayCashbackComponent>
}