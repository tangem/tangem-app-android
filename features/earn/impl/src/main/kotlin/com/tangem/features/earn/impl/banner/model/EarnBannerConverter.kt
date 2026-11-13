package com.tangem.features.earn.impl.banner.model

import com.tangem.core.res.R
import com.tangem.core.ui.extensions.TextReference
import com.tangem.core.ui.extensions.combinedReference
import com.tangem.core.ui.extensions.orMaskWithStars
import com.tangem.core.ui.extensions.pluralReference
import com.tangem.core.ui.extensions.resourceReference
import com.tangem.core.ui.extensions.stringReference
import com.tangem.core.ui.extensions.wrappedList
import com.tangem.core.ui.format.bigdecimal.crypto
import com.tangem.core.ui.format.bigdecimal.format
import com.tangem.domain.models.currency.CryptoCurrency
import com.tangem.features.earn.api.overview.EarnBannerOverview
import com.tangem.features.earn.api.overview.EarnOverview
import com.tangem.features.earn.api.overview.EarnProductRate
import com.tangem.features.earn.api.overview.EarnRateType
import com.tangem.features.earn.impl.banner.ui.state.EarnBannerUM
import com.tangem.features.earn.product.api.rate.formatEarnRate
import com.tangem.utils.StringsSigns
import javax.inject.Inject

internal class EarnBannerConverter @Inject constructor() {

    fun convert(
        overview: EarnOverview?,
        currency: CryptoCurrency,
        isBalanceHidden: Boolean,
        intents: EarnBannerClickIntents,
    ): EarnBannerUM {
        val banner = overview?.banner ?: return EarnBannerUM.Hidden
        return if (banner.activeCount > 0) {
            enabled(banner = banner, currency = currency, isBalanceHidden = isBalanceHidden, intents = intents)
        } else {
            offer(banner, intents)
        }
    }

    private fun offer(banner: EarnBannerOverview, intents: EarnBannerClickIntents) = EarnBannerUM.Offer(
        title = resourceReference(R.string.token_details_earn_title),
        description = banner.rate?.let(::upTo),
        buttonText = resourceReference(R.string.common_enable),
        onEnableClick = { intents.onEnableClick(banner.bestProductId) },
    )

    private fun enabled(
        banner: EarnBannerOverview,
        currency: CryptoCurrency,
        isBalanceHidden: Boolean,
        intents: EarnBannerClickIntents,
    ) = EarnBannerUM.Enabled(
        title = resourceReference(R.string.token_details_yield_enabled_title),
        description = enabledDescription(
            amount = banner.totalPosition?.format { crypto(currency) }?.orMaskWithStars(isBalanceHidden),
            activeCount = banner.activeCount,
        ),
        onClick = intents::onBannerClick,
    )

    private fun upTo(rate: EarnProductRate): TextReference {
        val percent = "${rate.fraction.formatEarnRate()}%"
        return when (rate.type) {
            EarnRateType.APY -> resourceReference(
                id = R.string.token_details_earn_staking_subtitle_apy,
                formatArgs = wrappedList(percent),
            )
            EarnRateType.APR -> resourceReference(
                id = R.string.token_details_earn_staking_subtitle,
                formatArgs = wrappedList(percent),
            )
        }
    }

    private fun enabledDescription(amount: String?, activeCount: Int): TextReference {
        val active = pluralReference(
            id = R.plurals.token_details_yield_active_count,
            count = activeCount,
            formatArgs = wrappedList(activeCount),
        )
        return if (amount == null) {
            active
        } else {
            combinedReference(stringReference(amount), stringReference(ACTIVE_SEPARATOR), active)
        }
    }

    private companion object {
        const val ACTIVE_SEPARATOR = " ${StringsSigns.MIDDLE_DOT} "
    }
}