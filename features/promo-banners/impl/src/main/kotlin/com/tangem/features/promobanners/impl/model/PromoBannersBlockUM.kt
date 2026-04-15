package com.tangem.features.promobanners.impl.model

import com.tangem.features.promobanners.api.PromoBannersBlockComponent
import kotlinx.collections.immutable.ImmutableList

internal data class PromoBannersBlockUM(
    val userWalletId: String,
    val initialPage: Int,
    val banners: ImmutableList<PromoBannerNotificationUM>,
    val isVisibleOnScreen: Boolean,
    val placeholder: PromoBannersBlockComponent.Placeholder,
    val onBannerShown: (displayId: Int) -> Unit,
    val onCarouselScrolled: (displayId: Int) -> Unit,
    val onPageChanged: (displayId: Int) -> Unit,
)