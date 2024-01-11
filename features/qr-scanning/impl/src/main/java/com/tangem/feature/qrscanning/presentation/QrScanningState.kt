package com.tangem.feature.qrscanning.presentation

import androidx.compose.runtime.Immutable
import com.tangem.core.ui.extensions.TextReference

@Immutable
data class QrScanningState(
    val message: TextReference?,
    val onQrScanned: (String) -> Unit,
    val onBackClick: () -> Unit,
    val onGalleryClicked: () -> Unit,
)