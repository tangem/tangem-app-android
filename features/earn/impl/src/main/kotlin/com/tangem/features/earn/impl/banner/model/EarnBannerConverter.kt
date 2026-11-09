package com.tangem.features.earn.impl.banner.model

import com.tangem.core.ui.extensions.orMaskWithStars
import com.tangem.core.ui.format.bigdecimal.crypto
import com.tangem.core.ui.format.bigdecimal.format
import com.tangem.domain.models.currency.CryptoCurrency
import com.tangem.features.earn.api.overview.EarnBannerOverview
import com.tangem.features.earn.api.overview.EarnOverview
import com.tangem.features.earn.impl.banner.EarnBannerTexts
import com.tangem.features.earn.impl.banner.ui.state.EarnBannerUM
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
        title = EarnBannerTexts.title,
        description = banner.rate?.let(EarnBannerTexts::upTo),
        buttonText = EarnBannerTexts.enable,
        onEnableClick = { intents.onEnableClick(banner.bestProductId) },
    )

    private fun enabled(
        banner: EarnBannerOverview,
        currency: CryptoCurrency,
        isBalanceHidden: Boolean,
        intents: EarnBannerClickIntents,
    ) = EarnBannerUM.Enabled(
        title = EarnBannerTexts.enabledTitle,
        description = EarnBannerTexts.enabledDescription(
            amount = banner.totalPosition?.format { crypto(currency) }?.orMaskWithStars(isBalanceHidden),
            activeCount = banner.activeCount,
        ),
        onClick = intents::onBannerClick,
    )
}