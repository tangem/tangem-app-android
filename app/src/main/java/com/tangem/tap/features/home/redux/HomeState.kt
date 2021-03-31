package com.tangem.tap.features.home.redux

import com.tangem.tap.common.redux.global.StateDialog
import org.rekotlin.StateType

data class HomeState(
        val firstLaunch: Boolean = true,
        val dialog: StateDialog? = null
) : StateType

sealed class HomeDialog: StateDialog {
    object ScanFailsDialog: HomeDialog()
}