package com.tangem.features.swap.v2.impl.amount

import com.tangem.core.ui.extensions.TextReference
import com.tangem.domain.models.currency.CryptoCurrency
import com.tangem.domain.swap.models.SwapDirection
import com.tangem.domain.tokens.model.CryptoCurrencyStatus
import com.tangem.domain.wallets.models.UserWallet
import com.tangem.features.swap.v2.impl.amount.entity.SwapAmountUM
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow

internal sealed class SwapAmountComponentParams {

    abstract val amountUM: SwapAmountUM
    abstract val analyticsCategoryName: String
    abstract val userWallet: UserWallet
    abstract val swapDirection: SwapDirection
    abstract val isBalanceHidingFlow: StateFlow<Boolean>
    abstract val primaryCryptoCurrencyStatusFlow: StateFlow<CryptoCurrencyStatus>
    abstract val secondaryCryptoCurrency: CryptoCurrency?

    data class AmountParams(
        override val amountUM: SwapAmountUM,
        override val analyticsCategoryName: String,
        override val userWallet: UserWallet,
        override val isBalanceHidingFlow: StateFlow<Boolean>,
        override val swapDirection: SwapDirection,
        override val primaryCryptoCurrencyStatusFlow: StateFlow<CryptoCurrencyStatus>,
        override val secondaryCryptoCurrency: CryptoCurrency?,
        val title: TextReference,
        val callback: SwapAmountComponent.ModelCallback,
        val currentRoute: Flow<SwapAmountRoute>,
    ) : SwapAmountComponentParams()

    data class AmountBlockParams(
        override val amountUM: SwapAmountUM,
        override val analyticsCategoryName: String,
        override val userWallet: UserWallet,
        override val isBalanceHidingFlow: StateFlow<Boolean>,
        override val swapDirection: SwapDirection,
        override val primaryCryptoCurrencyStatusFlow: StateFlow<CryptoCurrencyStatus>,
        override val secondaryCryptoCurrency: CryptoCurrency?,
        val blockClickEnableFlow: StateFlow<Boolean>,
    ) : SwapAmountComponentParams()
}