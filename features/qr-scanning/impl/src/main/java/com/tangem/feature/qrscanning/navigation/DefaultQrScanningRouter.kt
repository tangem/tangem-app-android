package com.tangem.feature.qrscanning.navigation

import androidx.fragment.app.Fragment
import com.tangem.common.routing.AppRouter

import com.tangem.feature.qrscanning.QrScanningFragment

class DefaultQrScanningRouter(
    private val router: AppRouter,
) : QrScanningInnerRouter {
    override fun getEntryFragment(): Fragment = QrScanningFragment.create()

    override fun popBackStack() {
        router.pop()
    }
}