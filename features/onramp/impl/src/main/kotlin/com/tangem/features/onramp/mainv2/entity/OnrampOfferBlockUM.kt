package com.tangem.features.onramp.mainv2.entity

import androidx.compose.runtime.Immutable
import com.tangem.core.ui.extensions.TextReference
import com.tangem.domain.onramp.model.OnrampPaymentMethod
import kotlinx.collections.immutable.ImmutableList

@Immutable
internal sealed interface OnrampOffersBlockUM {

    val isBlockVisible: Boolean

    data object Empty : OnrampOffersBlockUM {
        override val isBlockVisible: Boolean
            get() = false
    }

    data class Loading(
        override val isBlockVisible: Boolean,
    ) : OnrampOffersBlockUM

    data class Content(
        override val isBlockVisible: Boolean,
        val recentOffer: OnrampOfferUM?,
        val recommended: ImmutableList<OnrampOfferUM>,
        val onrampAllOffersButtonConfig: OnrampAllOffersButtonConfig?,
    ) : OnrampOffersBlockUM
}

internal data class OnrampOfferUM(
    val category: OnrampOfferCategoryUM,
    val advantages: OnrampOfferAdvantagesUM,
    val paymentMethod: OnrampPaymentMethod,
    val providerId: String,
    val providerName: String,
    val rate: String,
    val diff: TextReference?,
    val onBuyClicked: () -> Unit,
)

internal enum class OnrampOfferCategoryUM {
    RecentlyUsed, Recommended
}

internal enum class OnrampOfferAdvantagesUM {
    Default, BestRate, Fastest
}

internal data class OnrampAllOffersButtonConfig(
    val title: TextReference,
    val onClick: () -> Unit,
)