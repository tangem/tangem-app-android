package com.tangem.data.yield.supply.promo.converter

import com.tangem.datasource.api.promotion.models.PromotionsResponse
import com.tangem.domain.yield.supply.models.YieldBoostPromo
import kotlinx.datetime.Instant

internal object YieldBoostPromoConverter {

    private const val ACTIVE_STATUS = "active"

    fun convert(dto: PromotionsResponse.PromotionDto): YieldBoostPromo {
        val all = dto.all ?: return YieldBoostPromo.None
        if (!all.status.equals(ACTIVE_STATUS, ignoreCase = true)) return YieldBoostPromo.None

        val start = runCatching { Instant.parse(all.timeline.start) }.getOrNull() ?: return YieldBoostPromo.None
        val end = runCatching { Instant.parse(all.timeline.end) }.getOrNull() ?: return YieldBoostPromo.None

        val tokens = all.tokens.orEmpty().map { token ->
            YieldBoostPromo.Active.PromoToken(
                contractAddress = token.tokenAddress,
                tokenSymbol = token.tokenSymbol,
                tokenName = token.tokenName,
                networkId = token.networkId,
            )
        }
        if (tokens.isEmpty()) return YieldBoostPromo.None

        return YieldBoostPromo.Active(
            tokens = tokens,
            timeline = YieldBoostPromo.Active.Timeline(start = start, end = end),
            link = all.link,
        )
    }
}