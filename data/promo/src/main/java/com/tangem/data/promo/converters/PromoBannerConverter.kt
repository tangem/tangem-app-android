package com.tangem.data.promo.converters

import com.tangem.datasource.api.promotion.models.PromoBannerResponse
import com.tangem.domain.promo.models.PromoBanner
import com.tangem.utils.converter.Converter
import org.joda.time.DateTime

class PromoBannerConverter : Converter<PromoBannerResponse, PromoBanner?> {

    override fun convert(value: PromoBannerResponse): PromoBanner? {
        val bannerState = value.bannerState ?: return null
        return PromoBanner(
            name = value.name,
            bannerState = PromoBanner.BannerState(
                status = bannerState.status,
                link = bannerState.link,
                timeline = PromoBanner.Timeline(
                    start = DateTime.parse(bannerState.timeline.start),
                    end = DateTime.parse(bannerState.timeline.end),
                ),
            ),
        )
    }
}