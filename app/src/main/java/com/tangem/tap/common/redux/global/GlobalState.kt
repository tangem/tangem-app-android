package com.tangem.tap.common.redux.global

import com.tangem.blockchain.common.WalletManager
import com.tangem.commands.Card
import org.rekotlin.StateType

data class GlobalState(
        val card: Card? = null,
        val walletManager: WalletManager? = null,
) : StateType



