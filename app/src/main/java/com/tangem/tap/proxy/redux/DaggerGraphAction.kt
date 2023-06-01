package com.tangem.tap.proxy.redux

import com.tangem.domain.card.ScanCardUseCase
import com.tangem.features.tester.api.TesterRouter
import com.tangem.features.wallet.navigation.WalletRouter
import org.rekotlin.Action

sealed interface DaggerGraphAction : Action {

    data class SetActivityDependencies(
        val testerRouter: TesterRouter,
        val scanCardUseCase: ScanCardUseCase,
        val walletRouter: WalletRouter,
    ) : DaggerGraphAction
}