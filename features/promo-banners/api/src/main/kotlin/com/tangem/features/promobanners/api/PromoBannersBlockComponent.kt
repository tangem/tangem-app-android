package com.tangem.features.promobanners.api

import com.tangem.core.decompose.factory.ComponentFactory
import com.tangem.core.ui.decompose.ComposableContentComponent
import com.tangem.domain.models.wallet.UserWalletId

interface PromoBannersBlockComponent : ComposableContentComponent {

    data class Params(
        val userWalletId: UserWalletId,
        val placeholder: Placeholder,
    )

    enum class Placeholder(val value: String) {
        MAIN("main"),
        FEED("shtorka"),
    }

    interface Factory : ComponentFactory<Params, PromoBannersBlockComponent>
}