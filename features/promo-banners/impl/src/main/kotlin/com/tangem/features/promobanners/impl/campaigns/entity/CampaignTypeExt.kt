package com.tangem.features.promobanners.impl.campaigns.entity

import com.tangem.core.ui.ds.image.TangemIconUM
import com.tangem.core.ui.extensions.resourceReference
import com.tangem.domain.promo.models.PromoCampaignId
import com.tangem.features.promobanners.impl.R
import com.tangem.features.promobanners.impl.campaigns.model.CampaignContent
import com.tangem.utils.converter.Converter

internal class CampaignTypeToContentConverter : Converter<CampaignType, CampaignContent> {

    override fun convert(value: CampaignType): CampaignContent = when (value) {
        is CampaignType.ReactivationCashback -> CampaignContent(
            name = "Summer Swap Cashback",
            logo = TangemIconUM.Url(
                url = "https://s3.dualstack.eu-central-1.amazonaws.com/tangem.api/stories/Reactivation_Cashback.webp",
                fallbackRes = R.drawable.ic_alert_24,
            ),
            description = resourceReference(R.string.promo_campaign_reactivation_summary_description),
            termsUrl = "https://tangem.com/docs/en/summer-swap-cashback-terms.pdf",
            learnMoreUrl = "https://tangem.com/en/blog/post/summer-swap",
        )
        is CampaignType.WhaleSwapCashback -> CampaignContent(
            name = "Whale Swap Cashback",
            logo = TangemIconUM.Url(
                url = "https://s3.dualstack.eu-central-1.amazonaws.com/tangem.api/stories/Whale_Swap_Cashback.webp",
                fallbackRes = R.drawable.ic_alert_24,
            ),
            description = resourceReference(R.string.promo_campaign_whale_swap_summary_description),
            termsUrl = "https://tangem.com/docs/en/whale-swap-cashback-terms.pdf",
            learnMoreUrl = "https://tangem.com/en/blog/post/whale-swap",
        )
    }
}

internal fun CampaignType.toPromoCampaignId(): PromoCampaignId = when (this) {
    is CampaignType.ReactivationCashback -> PromoCampaignId.ReactivationCashback
    is CampaignType.WhaleSwapCashback -> PromoCampaignId.WhaleSwapCashback
}