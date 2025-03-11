package com.tangem.feature.qrscanning.presentation.transformers

import com.tangem.feature.qrscanning.presentation.QrScanningState

internal class DismissBottomSheetTransformer : QrScanningTransformer {
    override fun transform(prevState: QrScanningState): QrScanningState {
        return prevState.copy(
            bottomSheetConfig = null,
        )
    }
}