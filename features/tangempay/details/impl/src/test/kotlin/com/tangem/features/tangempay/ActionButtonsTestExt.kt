package com.tangem.features.tangempay

import com.tangem.features.tangempay.account.TangemPayAction
import com.tangem.features.tangempay.account.TangemPayActionButtonUM
import com.tangem.features.tangempay.account.TangemPayDetailsUM

internal val List<TangemPayActionButtonUM>.withdrawButton: TangemPayActionButtonUM
    get() = first { it.action == TangemPayAction.Withdraw }

internal val List<TangemPayActionButtonUM>.addFundsButton: TangemPayActionButtonUM
    get() = first { it.action == TangemPayAction.AddFunds }

internal val TangemPayDetailsUM.withdrawButton: TangemPayActionButtonUM
    get() = balanceBlockState.actionButtons.withdrawButton

internal val TangemPayDetailsUM.addFundsButton: TangemPayActionButtonUM
    get() = balanceBlockState.actionButtons.addFundsButton