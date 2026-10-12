package com.tangem.features.tangempay.withdraw

import com.tangem.common.routing.AppRoute
import com.tangem.common.routing.AppRoute.Swap.AccountFlow
import com.tangem.core.analytics.models.AnalyticsParam
import com.tangem.domain.models.currency.CryptoCurrency
import com.tangem.domain.models.wallet.UserWalletId
import com.tangem.utils.annotations.RemoveWithToggle

@RemoveWithToggle(toggleName = "AND_16842_TANGEM_PAY_BANK_OFFRAMP_ENABLED", description = "inline in call site")
internal fun buildWithdrawSwapRoute(
    userWalletId: UserWalletId,
    cryptoCurrency: CryptoCurrency.Token,
    toCryptoCurrency: CryptoCurrency? = null,
): AppRoute.Swap = AppRoute.Swap(
    fromCryptoCurrency = cryptoCurrency,
    userWalletId = userWalletId,
    screenSource = AnalyticsParam.ScreensSources.TangemPay.value,
    fromCurrencyPosition = AppRoute.Swap.CurrencyPosition.FROM,
    accountFlow = AccountFlow.Withdraw,
    toCryptoCurrency = toCryptoCurrency,
)