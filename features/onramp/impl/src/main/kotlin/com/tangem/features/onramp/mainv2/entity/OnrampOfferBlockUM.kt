package com.tangem.features.onramp.mainv2.entity

import androidx.compose.runtime.Immutable
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
        val offers: ImmutableList<OnrampOfferUM>,
    ) : OnrampOffersBlockUM
}

internal data class OnrampOfferUM(
    val category: OnrampOfferCategory,
    val advantages: OnrampOfferAdvantages,
    val paymentMethod: OnrampPaymentMethod,
    val providerId: String,
    val providerName: String,
)

internal enum class OnrampOfferCategory {
    RecentlyUsed, Recommended
}

internal enum class OnrampOfferAdvantages {
    Default, BestRate, Fastest
}