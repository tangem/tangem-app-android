package com.tangem.features.tokenreceive.ui.state

import com.tangem.core.ui.extensions.TextReference

internal data class QrCodeUM(
    val onCopyClick: () -> Unit,
    val onShareClick: (String) -> Unit,
    val addressName: TextReference,
    val addressValue: String,
    val network: String,
)