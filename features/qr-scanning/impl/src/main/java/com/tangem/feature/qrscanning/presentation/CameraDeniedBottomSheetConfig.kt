package com.tangem.feature.qrscanning.presentation

import com.tangem.core.ui.components.bottomsheets.TangemBottomSheetConfigContent

data class CameraDeniedBottomSheetConfig(
    val onGalleryClick: () -> Unit,
    val onCancelClick: () -> Unit,
) : TangemBottomSheetConfigContent