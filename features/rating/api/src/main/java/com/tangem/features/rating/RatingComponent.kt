package com.tangem.features.rating

import com.tangem.core.decompose.context.AppComponentContext
import com.tangem.core.ui.decompose.ComposableContentComponent
import com.tangem.domain.models.wallet.UserWalletId

interface RatingComponent : ComposableContentComponent {

    class Params(
        val txExternalId: String,
        val providerName: String,
        val txExternalUrl: String,
        val userWalletId: UserWalletId,
        val isRedesign: Boolean = false,
    )

    interface Factory {
        fun create(context: AppComponentContext, params: Params): RatingComponent
    }
}