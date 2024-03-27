package com.tangem.feature.qrscanning.presentation.transformers

import com.tangem.core.ui.extensions.resourceReference
import com.tangem.core.ui.extensions.wrappedList
import com.tangem.domain.qrscanning.models.SourceType
import com.tangem.feature.qrscanning.impl.R
import com.tangem.feature.qrscanning.presentation.QrScanningState
import com.tangem.feature.qrscanning.viewmodel.QrScanningClickIntents

internal class InitializeQrScanningStateTransformer(
    private val clickIntents: QrScanningClickIntents,
    private val source: SourceType,
    private val network: String?,
) : QrScanningTransformer {

    override fun transform(prevState: QrScanningState): QrScanningState {
        val message = when (source) {
            SourceType.SEND -> network?.let { resourceReference(R.string.send_qrcode_scan_info, wrappedList(it)) }
            else -> null
        }

        return QrScanningState(
            message = message,
            onBackClick = clickIntents::onBackClick,
            onQrScanned = clickIntents::onQrScanned,
            onGalleryClick = clickIntents::onGalleryClicked,
        )
    }
}