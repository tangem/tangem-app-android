package com.tangem.tap.features.walletSelector.redux

import com.tangem.common.core.TangemError
import com.tangem.domain.common.util.UserWalletId
import com.tangem.tap.common.entities.FiatCurrency
import org.rekotlin.StateType

data class WalletSelectorState(
    val wallets: List<UserWalletModel> = emptyList(),
    val selectedWalletId: UserWalletId? = null,
    val isLocked: Boolean = false,
    val fiatCurrency: FiatCurrency = FiatCurrency.Default,
    val isCardSavingInProgress: Boolean = false,
    val isUnlockInProgress: Boolean = false,
    val error: TangemError? = null,
) : StateType
