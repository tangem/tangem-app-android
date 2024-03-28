package com.tangem.core.navigation

interface StateDialog {

    data class ScanFailsDialog(val source: ScanFailsSource) : StateDialog

    enum class ScanFailsSource {
        MAIN, SIGN_IN, SETTINGS, INTRO;
    }
}
