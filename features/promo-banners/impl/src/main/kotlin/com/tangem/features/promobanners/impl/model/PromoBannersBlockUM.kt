package com.tangem.features.promobanners.impl.model

import kotlinx.collections.immutable.ImmutableList

internal data class PromoBannersBlockUM(
    val userWalletId: String,
    val initialPage: Int,
    val banners: ImmutableList<PromoBannerNotificationUM>,
    val onBannerShown: (displayId: Int) -> Unit,
    val onCarouselScrolled: (displayId: Int) -> Unit,
    val onPageChanged: (displayId: Int) -> Unit,
)