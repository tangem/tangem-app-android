package com.tangem.common.ui.cosign

import androidx.compose.runtime.Immutable
import com.tangem.core.ui.ds.image.TangemIconUM
import com.tangem.core.ui.extensions.TextReference

/**
 * A [com.tangem.core.ui.ds2.messagebanner.TangemMessageBanner]-shaped cosign request slot.
 * [progress] and [trailingIcon] are mutually exclusive; when both are `null` no trailing slot is
 * rendered.
 */
@Immutable
data class CosignRequestBannerUM(
    val title: TextReference,
    val subtitle: TextReference,
    val progress: SignatureProgressUM? = null,
    val trailingIcon: TangemIconUM? = null,
    val onClick: (() -> Unit)? = null,
)