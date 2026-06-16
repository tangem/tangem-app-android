package com.tangem.features.swap

import com.tangem.core.decompose.factory.ComponentFactory
import com.tangem.core.ui.decompose.ComposableContentComponent
import com.tangem.domain.models.currency.CryptoCurrency
import com.tangem.domain.models.wallet.UserWalletId
import java.math.BigDecimal

interface SwapComponent : ComposableContentComponent {

    data class Params(
        val userWalletId: UserWalletId,
        val cryptoCurrency: CryptoCurrency? = null,
        val screenSource: String,
        val currencyPosition: CurrencyPosition = CurrencyPosition.ANY,
        val tangemPayInput: TangemPayInput? = null,
    ) {
        data class TangemPayInput(
            val cryptoAmount: BigDecimal,
            val fiatAmount: BigDecimal,
            val depositAddress: String,
        )

        /** Preferred position of the pre-selected currency on the swap screen. */
        enum class CurrencyPosition {
            /** Force-place as the FROM (send) currency. */
            FROM,
            /** Force-place as the TO (receive) currency. */
            TO,
            /** Auto-determine position based on availability and balance. */
            ANY,
        }
    }

    interface Factory : ComponentFactory<Params, SwapComponent>
}