package com.tangem.common.ui.userwallet.ext

import com.tangem.common.ui.userwallet.state.UserWalletItemUM
import com.tangem.core.ui.format.bigdecimal.fiat
import com.tangem.core.ui.format.bigdecimal.format
import com.tangem.domain.appcurrency.model.AppCurrency
import com.tangem.domain.models.StatusSource
import com.tangem.domain.models.TotalFiatBalance

internal fun TotalFiatBalance?.toUserWalletItemBalance(
    appCurrency: AppCurrency?,
    isBalanceHidden: Boolean,
): UserWalletItemUM.Balance = when {
    isBalanceHidden -> UserWalletItemUM.Balance.Hidden
    this == null -> UserWalletItemUM.Balance.Loading
    this is TotalFiatBalance.Loading -> UserWalletItemUM.Balance.Loading
    this is TotalFiatBalance.Failed -> UserWalletItemUM.Balance.Failed
    this is TotalFiatBalance.Loaded && appCurrency != null -> UserWalletItemUM.Balance.Loaded(
        value = amount.format { fiat(appCurrency.code, appCurrency.symbol) },
        isFlickering = source == StatusSource.CACHE,
    )
    else -> UserWalletItemUM.Balance.Failed
}