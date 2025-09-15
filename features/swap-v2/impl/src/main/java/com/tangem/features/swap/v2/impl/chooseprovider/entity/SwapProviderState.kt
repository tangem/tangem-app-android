package com.tangem.features.swap.v2.impl.chooseprovider.entity

import androidx.compose.runtime.Immutable
import com.tangem.core.ui.extensions.TextReference
import com.tangem.features.swap.v2.impl.common.entity.SwapQuoteUM.Content.DifferencePercent

@Deprecated("Use ProviderChooseUM with new design")
@Immutable
internal sealed class SwapProviderState {

    abstract val isSelected: Boolean

    data object Empty : SwapProviderState() {
        override val isSelected = false
    }

    data class Content(
        override val isSelected: Boolean,
        val name: String,
        val type: String,
        val iconUrl: String,
        val subtitle: TextReference,
        val additionalBadge: AdditionalBadge,
        val diffPercent: DifferencePercent,
    ) : SwapProviderState()

    @Immutable
    sealed class AdditionalBadge {
        data object FCAWarningList : AdditionalBadge()
        data object BestTrade : AdditionalBadge()
        data object Empty : AdditionalBadge()
        data object PermissionRequired : AdditionalBadge()
    }
}