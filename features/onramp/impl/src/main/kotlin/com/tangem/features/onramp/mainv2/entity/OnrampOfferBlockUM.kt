package com.tangem.features.onramp.mainv2.entity

import androidx.compose.runtime.Immutable
import com.tangem.core.ui.extensions.TextReference
import com.tangem.domain.onramp.analytics.OnrampAnalyticsEvent
import com.tangem.domain.onramp.model.OnrampPaymentMethod
import kotlinx.collections.immutable.ImmutableList

@Immutable
internal sealed interface OnrampOffersBlockUM {

    data object Empty : OnrampOffersBlockUM

    data object Loading : OnrampOffersBlockUM

    data class Content(
        val recentOffer: OnrampOfferUM?,
        val recommended: ImmutableList<OnrampOfferUM>,
        val onrampAllOffersButtonConfig: OnrampAllOffersButtonConfig?,
    ) : OnrampOffersBlockUM
}

internal data class OnrampOfferUM(
    val category: OnrampOfferCategoryUM,
    val advantages: OnrampOfferAdvantagesUM,
    val paymentMethod: OnrampPaymentMethod,
    val providerName: String,
    val rate: String,
    val diff: TextReference?,
    val onBuyClicked: () -> Unit,
)

internal enum class OnrampOfferCategoryUM {
    RecentlyUsed, Recommended
}

internal sealed interface OnrampOfferAdvantagesUM {
    data object Default : OnrampOfferAdvantagesUM
    data object BestRate : OnrampOfferAdvantagesUM
    data object GreatRate : OnrampOfferAdvantagesUM
    data object Fastest : OnrampOfferAdvantagesUM

    sealed interface Unavailable : OnrampOfferAdvantagesUM {
        data object MinAmount : Unavailable
        data object MaxAmount : Unavailable
    }

    fun toAnalyticsEvent(
        cryptoCurrencySymbol: String,
        providerName: String,
        paymentMethodName: String,
    ): OnrampAnalyticsEvent? {
        return when (this) {
            GreatRate -> OnrampAnalyticsEvent.BestRateClicked(
                tokenSymbol = cryptoCurrencySymbol,
                providerName = providerName,
                paymentMethod = paymentMethodName,
            )
            Fastest -> OnrampAnalyticsEvent.FastestBuyMethodClicked(
                tokenSymbol = cryptoCurrencySymbol,
                providerName = providerName,
                paymentMethod = paymentMethodName,
            )
            Default,
            BestRate,
            is Unavailable,
            -> null
        }
    }
}

internal data class OnrampAllOffersButtonConfig(
    val title: TextReference,
    val onClick: () -> Unit,
)