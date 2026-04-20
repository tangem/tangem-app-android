package com.tangem.common.ui.markets.action

import com.tangem.domain.models.currency.CryptoCurrencyStatus
import com.tangem.domain.models.wallet.UserWallet
import com.tangem.domain.tokens.model.TokenActionsState

data class CryptoCurrencyData(
    val userWallet: UserWallet,
    val status: CryptoCurrencyStatus,
    val actions: List<TokenActionsState.ActionState>,
)