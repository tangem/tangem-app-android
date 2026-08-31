package com.tangem.common.ui.cosign

import androidx.compose.runtime.Immutable
import com.tangem.core.ui.extensions.TextReference

/** A compact cosign request row with a signature-progress avatar stack (Figma: "💠 Row"). */
@Immutable
data class CosignRequestRowUM(
    val title: TextReference,
    val progress: SignatureProgressUM,
    val onInfoClick: (() -> Unit)? = null,
    val onClick: (() -> Unit)? = null,
)