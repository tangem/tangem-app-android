package com.tangem.features.promobanners.impl.model

import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf

internal data class PromoBannersBlockUM(
    val banners: ImmutableList<PromoBannerNotificationUM> = persistentListOf(),
)