package com.tangem.domain.promo.models

import org.joda.time.DateTime

data class PromoBanner(
    val name: String,
    val bannerState: BannerState,
) {

    val isActive = bannerState.status == ACTIVE_STATUS && bannerState.timeline.end.isAfterNow

    data class BannerState(
        val timeline: Timeline,
        val status: String,
        val link: String?,
    )

    data class Timeline(
        val start: DateTime,
        val end: DateTime,
    )

    private companion object {
        const val ACTIVE_STATUS = "active"
    }
}