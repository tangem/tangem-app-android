package com.tangem.feature.qrscanning.viewmodel

interface QrScanningClickIntents {

    fun onBackClick()

    fun onQrScanned(qrCode: String)

    fun onGalleryClicked()
}