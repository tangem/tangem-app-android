package com.tangem.feature.qr_scanning

import androidx.fragment.app.Fragment

interface QrScanRouter {

    fun getEntryFragment(type: SourceType): Fragment

    companion object {
        const val SOURCE_KEY = "source"
    }
}
