package com.tangem.tap.common

/**
[REDACTED_AUTHOR]
 */

object TestActions {

    // It used only for the test actions in debug or debug_beta builds
    var testAmountInjectionForWalletManagerEnabled = false
}

typealias TestAction = Pair<String, () -> Unit>