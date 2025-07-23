package com.tangem.feature.qrscanning.model

import com.tangem.domain.qrscanning.models.QrResultSource
import kotlinx.coroutines.flow.SharedFlow

internal interface QrScanningClickIntents {

    val launchGallery: SharedFlow<Unit>

    fun onBackClick()

    fun onQrScanned(qrCode: String, source: QrResultSource)

    fun onGalleryClicked()

    fun onSettingsClick()
}