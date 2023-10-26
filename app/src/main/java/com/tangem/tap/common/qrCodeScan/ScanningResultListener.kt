package com.tangem.tap.common.qrCodeScan

interface ScanningResultListener {

    fun onScanned(result: String)
}
