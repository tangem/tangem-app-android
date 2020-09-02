package com.tangem.tap.common.redux.global

import com.tangem.blockchain.common.WalletManager
import com.tangem.commands.Card
import org.rekotlin.Action
import java.math.BigDecimal

sealed class GlobalAction : Action {

    data class LoadCard(val card: Card) : GlobalAction()
    data class LoadWalletManager(val walletManager: WalletManager) : GlobalAction()
    data class SetFiatRate(val fiatRates: Pair<String, BigDecimal>) : GlobalAction()
}