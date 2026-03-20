package com.tangem.features.promobanners.impl.model

import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf

internal data class PromoBannersBlockUM(
    val banners: ImmutableList<PromoBannerNotificationUM> = persistentListOf(),
    val onBannerShown: (displayId: String) -> Unit = {},
    val onCarouselScrolled: (displayId: String) -> Unit = {},
)