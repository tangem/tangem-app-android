package com.tangem.features.swap.v2.impl.amount

import com.tangem.core.ui.extensions.TextReference
import com.tangem.domain.express.models.ExpressProviderType
import com.tangem.domain.models.account.Account
import com.tangem.domain.models.currency.CryptoCurrency
import com.tangem.domain.models.currency.CryptoCurrencyStatus
import com.tangem.domain.models.wallet.UserWallet
import com.tangem.domain.swap.models.SwapDirection
import com.tangem.features.send.v2.api.analytics.CommonSendAnalyticEvents
import com.tangem.features.swap.v2.impl.amount.entity.SwapAmountUM
import com.tangem.features.swap.v2.impl.sendviaswap.SendWithSwapRoute
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow

internal sealed class SwapAmountComponentParams {

    abstract val amountUM: SwapAmountUM
    abstract val analyticsCategoryName: String
    abstract val analyticsSendSource: CommonSendAnalyticEvents.CommonSendSource
    abstract val userWallet: UserWallet
    abstract val swapDirection: SwapDirection
    abstract val isBalanceHidingFlow: StateFlow<Boolean>
    abstract val primaryCryptoCurrencyStatusFlow: StateFlow<CryptoCurrencyStatus>
    abstract val secondaryCryptoCurrency: CryptoCurrency?
    abstract val filterProviderTypes: List<ExpressProviderType>
    abstract val accountFlow: StateFlow<Account.CryptoPortfolio?>
    abstract val isAccountModeFlow: StateFlow<Boolean>

    data class AmountParams(
        override val amountUM: SwapAmountUM,
        override val analyticsCategoryName: String,
        override val userWallet: UserWallet,
        override val isBalanceHidingFlow: StateFlow<Boolean>,
        override val swapDirection: SwapDirection,
        override val primaryCryptoCurrencyStatusFlow: StateFlow<CryptoCurrencyStatus>,
        override val secondaryCryptoCurrency: CryptoCurrency?,
        override val filterProviderTypes: List<ExpressProviderType> = emptyList(),
        override val analyticsSendSource: CommonSendAnalyticEvents.CommonSendSource,
        override val accountFlow: StateFlow<Account.CryptoPortfolio?>,
        override val isAccountModeFlow: StateFlow<Boolean>,
        val title: TextReference,
        val callback: SwapAmountComponent.ModelCallback,
        val currentRoute: Flow<SendWithSwapRoute>,
    ) : SwapAmountComponentParams()

    data class AmountBlockParams(
        override val amountUM: SwapAmountUM,
        override val analyticsCategoryName: String,
        override val userWallet: UserWallet,
        override val isBalanceHidingFlow: StateFlow<Boolean>,
        override val swapDirection: SwapDirection,
        override val primaryCryptoCurrencyStatusFlow: StateFlow<CryptoCurrencyStatus>,
        override val secondaryCryptoCurrency: CryptoCurrency?,
        override val filterProviderTypes: List<ExpressProviderType> = emptyList(),
        override val analyticsSendSource: CommonSendAnalyticEvents.CommonSendSource,
        override val accountFlow: StateFlow<Account.CryptoPortfolio?>,
        override val isAccountModeFlow: StateFlow<Boolean>,
        val blockClickEnableFlow: StateFlow<Boolean>,
    ) : SwapAmountComponentParams()
}