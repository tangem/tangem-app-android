package com.tangem.tap.proxy.redux

import com.tangem.features.tester.api.TesterRouter
import org.rekotlin.Action

sealed interface DaggerGraphAction : Action {

    data class SetActivityDependencies(val testerRouter: TesterRouter) : DaggerGraphAction
}