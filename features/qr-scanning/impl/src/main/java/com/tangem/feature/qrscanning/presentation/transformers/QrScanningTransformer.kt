package com.tangem.feature.qrscanning.presentation.transformers

import com.tangem.feature.qrscanning.presentation.QrScanningState

internal interface QrScanningTransformer {

    fun transform(prevState: QrScanningState): QrScanningState
}