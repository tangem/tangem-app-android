package com.tangem.features.tangempay

import com.tangem.core.ui.components.buttons.actions.ActionButtonConfig
import com.tangem.features.tangempay.entity.TangemPayAction
import com.tangem.features.tangempay.entity.TangemPayActionButtonUM
import com.tangem.features.tangempay.entity.TangemPayDetailsUM

internal val List<TangemPayActionButtonUM>.withdrawButton: ActionButtonConfig
    get() = first { it.action == TangemPayAction.Withdraw }.config

internal val List<TangemPayActionButtonUM>.addFundsButton: ActionButtonConfig
    get() = first { it.action == TangemPayAction.AddFunds }.config

internal val TangemPayDetailsUM.withdrawButton: ActionButtonConfig
    get() = balanceBlockState.actionButtons.withdrawButton

internal val TangemPayDetailsUM.addFundsButton: ActionButtonConfig
    get() = balanceBlockState.actionButtons.addFundsButton