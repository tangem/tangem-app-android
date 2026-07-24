package com.tangem.features.promobanners.api

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Dp
import com.tangem.core.decompose.factory.ComponentFactory

@Immutable
interface PromoBannersBlockComponent {

    /**
     * @param walletId when non-null, renders the banners of that specific wallet (used by the wallet
     * pager so each page shows its own banners synchronously while swiping); when null, renders the
     * currently selected wallet's banners.
     */
    @Composable
    fun ContentWithPadding(horizontalItemPadding: Dp, walletId: String?, modifier: Modifier)

    fun setVisibleOnScreen(isVisible: Boolean)

    data class Params(
        val placeholder: Placeholder,
        val isInitiallyVisibleOnScreen: Boolean = true,
    )

    enum class Placeholder(val value: String) {
        MAIN("main"),
        FEED("shtorka"),
        PAYMENT_ACCOUNT_MAIN("payment_account_main"),
    }

    interface Factory : ComponentFactory<Params, PromoBannersBlockComponent>
}