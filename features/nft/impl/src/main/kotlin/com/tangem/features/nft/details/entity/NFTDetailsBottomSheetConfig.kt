package com.tangem.features.nft.details.entity

import com.tangem.core.ui.extensions.TextReference
import kotlinx.serialization.Serializable

@Serializable
internal sealed interface NFTDetailsBottomSheetConfig {
    @Serializable
    data class Info(
        val text: TextReference,
    ) : NFTDetailsBottomSheetConfig
}