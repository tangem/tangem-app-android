package com.tangem.feature.qrscanning.navigation

import androidx.fragment.app.Fragment
import com.tangem.core.navigation.ReduxNavController
import com.tangem.feature.qrscanning.QrScanningFragment

class DefaultQrScanningRouter(
    private val reduxNavController: ReduxNavController,
) : QrScanningInnerRouter {
    override fun getEntryFragment(): Fragment = QrScanningFragment.create()

    override fun popBackStack() {
        reduxNavController.popBackStack()
    }
}