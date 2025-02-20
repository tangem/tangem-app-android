package com.tangem.feature.qrscanning.presentation.transformers

import com.tangem.core.ui.components.bottomsheets.TangemBottomSheetConfig
import com.tangem.feature.qrscanning.presentation.CameraDeniedBottomSheetConfig
import com.tangem.feature.qrscanning.presentation.QrScanningState
import com.tangem.feature.qrscanning.model.QrScanningClickIntents

internal class ShowCameraDeniedBottomSheetTransformer(
    private val clickIntents: QrScanningClickIntents,
) : QrScanningTransformer {

    override fun transform(prevState: QrScanningState): QrScanningState {
        return prevState.copy(
            bottomSheetConfig = TangemBottomSheetConfig(
                isShown = true,
                onDismissRequest = clickIntents::onBackClick,
                content = CameraDeniedBottomSheetConfig(
                    onSettingsClick = clickIntents::onSettingsClick,
                    onCancelClick = clickIntents::onBackClick,
                    onGalleryClick = clickIntents::onGalleryClicked,
                ),
            ),
        )
    }
}