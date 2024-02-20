package com.tangem.feature.qrscanning.presentation.transformers

import com.tangem.core.ui.components.bottomsheets.TangemBottomSheetConfig
import com.tangem.feature.qrscanning.presentation.CameraDeniedBottomSheetConfig
import com.tangem.feature.qrscanning.presentation.QrScanningState
import com.tangem.feature.qrscanning.viewmodel.QrScanningClickIntents

internal class ShowCameraDeniedBottomSheetTransformer(
    private val clickIntents: QrScanningClickIntents,
) : QrScanningTransformer {

    override fun transform(prevState: QrScanningState): QrScanningState {
        return prevState.copy(
            bottomSheetConfig = TangemBottomSheetConfig(
                isShow = true,
                onDismissRequest = clickIntents::onBackClick,
                content = CameraDeniedBottomSheetConfig(
                    onCancelClick = clickIntents::onBackClick,
                    onGalleryClick = clickIntents::onGalleryClicked,
                ),
            ),
        )
    }
}