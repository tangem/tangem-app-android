package com.tangem.feature.qrscanning.model

import kotlinx.coroutines.flow.SharedFlow

internal interface QrScanningClickIntents {

    val launchGallery: SharedFlow<Unit>

    fun onBackClick()

    fun onQrScanned(qrCode: String)

    fun onGalleryClicked()

    fun onSettingsClick()
}