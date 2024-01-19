package com.tangem.feature.qrscanning.navigation

import com.tangem.feature.qrscanning.QrScanningRouter

interface QrScanningInnerRouter : QrScanningRouter {

    fun popBackStack()
}