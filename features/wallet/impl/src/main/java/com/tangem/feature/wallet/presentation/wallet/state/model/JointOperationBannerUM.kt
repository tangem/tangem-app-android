package com.tangem.feature.wallet.presentation.wallet.state.model

import androidx.compose.runtime.Immutable
import com.tangem.common.ui.cosign.CosignRequestBannerUM

@Immutable
internal data class JointOperationBannerUM(
    val operationId: String,
    val banner: CosignRequestBannerUM,
)