package com.tangem.domain.redux

interface StateDialog {

    data class ScanFailsDialog(val source: ScanFailsSource, val onTryAgain: (() -> Unit)? = null) : StateDialog

    enum class ScanFailsSource {
        MAIN, SIGN_IN, SETTINGS, INTRO;
    }
}
