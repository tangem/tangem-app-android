package com.tangem.features.tangempay.eligibility

import com.tangem.core.decompose.factory.ComponentFactory
import com.tangem.core.ui.decompose.ComposableBottomSheetComponent
import com.tangem.domain.models.pay.TangemPayProduct
import com.tangem.domain.models.wallet.UserWalletId

interface TangemPayWaitlistComponent : ComposableBottomSheetComponent {

    data class Params(
        val userWalletId: UserWalletId?,
        val product: TangemPayProduct,
        val countryCode: String,
        val regionCode: String?,
        val onDismiss: () -> Unit,
    )

    interface Factory : ComponentFactory<Params, TangemPayWaitlistComponent>
}