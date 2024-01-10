package com.tangem.feature.qrscanning

import androidx.fragment.app.Fragment

interface QrScanningRouter {

    fun getEntryFragment(): Fragment

    companion object {
        const val SOURCE_KEY = "source"
    }
}