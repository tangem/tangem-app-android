package com.tangem.feature.qrscanning

import androidx.fragment.app.Fragment

internal class DefaultQrScanRouter : QrScanRouter {

    override fun getEntryFragment(type: SourceType): Fragment = QrScanFragment.create(type)
}
