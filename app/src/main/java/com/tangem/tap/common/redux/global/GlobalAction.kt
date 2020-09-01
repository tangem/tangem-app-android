package com.tangem.tap.common.redux.global

import com.tangem.blockchain.common.WalletManager
import com.tangem.commands.Card
import org.rekotlin.Action

sealed class GlobalAction : Action {

    data class LoadCard(val card: Card) : GlobalAction()
    data class LoadWalletManager(val walletManager: WalletManager) : GlobalAction()
}