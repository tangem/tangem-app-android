package com.tangem.features.tangempay.withdraw

import com.tangem.common.routing.AppRoute
import com.tangem.common.routing.AppRoute.Swap.AccountFlow
import com.tangem.core.analytics.models.AnalyticsParam
import com.tangem.domain.models.currency.CryptoCurrency
import com.tangem.domain.models.wallet.UserWalletId

internal fun buildWithdrawSwapRoute(
    userWalletId: UserWalletId,
    cryptoCurrency: CryptoCurrency.Token,
    toCryptoCurrency: CryptoCurrency? = null,
    accountFlow: AccountFlow = AccountFlow.Withdraw,
    screenSource: String = AnalyticsParam.ScreensSources.TangemPay.value,
): AppRoute.Swap = AppRoute.Swap(
    fromCryptoCurrency = cryptoCurrency,
    userWalletId = userWalletId,
    screenSource = screenSource,
    fromCurrencyPosition = AppRoute.Swap.CurrencyPosition.FROM,
    accountFlow = accountFlow,
    toCryptoCurrency = toCryptoCurrency,
)