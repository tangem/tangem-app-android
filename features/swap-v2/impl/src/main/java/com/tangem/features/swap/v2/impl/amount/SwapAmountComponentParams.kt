package com.tangem.features.swap.v2.impl.amount

import com.tangem.domain.appcurrency.model.AppCurrency
import com.tangem.domain.models.currency.CryptoCurrency
import com.tangem.domain.swap.models.SwapDirection
import com.tangem.domain.tokens.model.CryptoCurrencyStatus
import com.tangem.domain.wallets.models.UserWallet
import com.tangem.features.swap.v2.impl.amount.entity.SwapAmountUM
import com.tangem.features.swap.v2.impl.swap.SwapRoute
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow

internal sealed class SwapAmountComponentParams {

    abstract val amountUM: SwapAmountUM
    abstract val analyticsCategoryName: String
    abstract val userWallet: UserWallet
    abstract val appCurrency: AppCurrency
    abstract val swapDirection: SwapDirection
    abstract val isBalanceHidingFlow: StateFlow<Boolean>

    data class AmountParams(
        override val amountUM: SwapAmountUM,
        override val analyticsCategoryName: String,
        override val userWallet: UserWallet,
        override val isBalanceHidingFlow: StateFlow<Boolean>,
        override val appCurrency: AppCurrency,
        override val swapDirection: SwapDirection,
        val primaryCryptoCurrency: CryptoCurrency,
        val secondaryCryptoCurrency: CryptoCurrency?,
        val callback: SwapAmountComponent.ModelCallback,
        val currentRoute: Flow<SwapRoute.Amount>,
        val onBackClick: () -> Unit,
        val onNextClick: () -> Unit,
    ) : SwapAmountComponentParams()

    data class AmountBlockParams(
        override val amountUM: SwapAmountUM,
        override val analyticsCategoryName: String,
        override val userWallet: UserWallet,
        override val isBalanceHidingFlow: StateFlow<Boolean>,
        override val appCurrency: AppCurrency,
        override val swapDirection: SwapDirection,
        val primaryCryptoCurrencyStatus: CryptoCurrencyStatus,
        val secondaryCryptoCurrencyStatus: CryptoCurrencyStatus?,
        val blockClickEnableFlow: StateFlow<Boolean>,
    ) : SwapAmountComponentParams()
}