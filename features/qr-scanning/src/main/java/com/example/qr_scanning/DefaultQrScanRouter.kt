package com.example.qr_scanning

import androidx.fragment.app.Fragment

internal class DefaultQrScanRouter : QrScanRouter {

    override fun getEntryFragment(): Fragment = QrScanFragment()

}

