package com.tangem.features.swap

import com.tangem.core.decompose.factory.ComponentFactory
import com.tangem.core.ui.decompose.ComposableContentComponent
import com.tangem.domain.models.account.Account
import com.tangem.domain.models.currency.CryptoCurrency
import com.tangem.domain.models.currency.CryptoCurrencyStatus
import com.tangem.domain.models.wallet.UserWalletId
import java.math.BigDecimal

interface SwapComponent : ComposableContentComponent {

    data class Params(
        val currencyFrom: CryptoCurrency,
        val currencyTo: CryptoCurrency? = null,
        val userWalletId: UserWalletId,
        val isInitialReverseOrder: Boolean = false,
        val screenSource: String,
        val tangemPayInput: TangemPayInput? = null,
        val preselectedToToken: CryptoCurrencyStatus? = null,
        val preselectedAccount: Account? = null,
    ) {
        data class TangemPayInput(
            val cryptoAmount: BigDecimal,
            val fiatAmount: BigDecimal,
            val depositAddress: String,
            val isWithdrawal: Boolean,
        )
    }

    interface Factory : ComponentFactory<Params, SwapComponent>
}