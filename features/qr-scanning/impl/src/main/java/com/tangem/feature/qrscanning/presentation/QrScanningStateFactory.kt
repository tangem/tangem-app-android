package com.tangem.feature.qrscanning.presentation

import com.tangem.core.ui.extensions.resourceReference
import com.tangem.core.ui.extensions.wrappedList
import com.tangem.feature.qrscanning.viewmodel.QrScanningClickIntents
import com.tangem.feature.qrscanning.SourceType
import com.tangem.feature.qrscanning.impl.R

internal class QrScanningStateFactory(
    val clickIntents: QrScanningClickIntents,
) {

    fun getInitialState(source: SourceType, network: String?): QrScanningState {
        val message = when (source) {
            SourceType.SEND -> network?.let { resourceReference(R.string.send_qrcode_scan_info, wrappedList(it)) }
            else -> null
        }

        return QrScanningState(
            message = message,
            onBackClick = clickIntents::onBackClick,
            onQrScanned = clickIntents::onQrScanned,
            onGalleryClicked = clickIntents::onGalleryClicked,
        )
    }
}