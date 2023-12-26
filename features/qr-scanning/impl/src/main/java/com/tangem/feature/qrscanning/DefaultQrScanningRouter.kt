package com.tangem.feature.qrscanning

import androidx.fragment.app.Fragment

internal class DefaultQrScanningRouter : QrScanningRouter {

    override fun getEntryFragment(): Fragment = QrScanningFragment.create()
}