package com.tangem.tap.common

/**
[REDACTED_AUTHOR]
 */

object TestActions {

    // It used only for the test actions in debug or debug_beta builds
    var isTestAmountInjectionForWalletManagerEnabled = false
}

typealias TestAction = Pair<String, () -> Unit>