package com.tangem.features.onboarding.v2.multiwallet.impl.child.chooseoption.ui

import com.tangem.core.ui.extensions.TextReference
import com.tangem.core.ui.extensions.resourceReference
import com.tangem.features.onboarding.v2.impl.R

internal val CarouselItems = listOf(
    CarouselItem(
        title = resourceReference(R.string.onboarding_wallet_info_title_first),
        subtitle = resourceReference(R.string.onboarding_wallet_info_subtitle_first),
    ),
    CarouselItem(
        title = resourceReference(R.string.onboarding_wallet_info_title_second),
        subtitle = resourceReference(R.string.onboarding_wallet_info_subtitle_second),
    ),
    CarouselItem(
        title = resourceReference(R.string.onboarding_wallet_info_title_third),
        subtitle = resourceReference(R.string.onboarding_wallet_info_subtitle_third),
    ),
    CarouselItem(
        title = resourceReference(R.string.onboarding_wallet_info_title_fourth),
        subtitle = resourceReference(R.string.onboarding_wallet_info_subtitle_fourth),
    ),
)

internal data class CarouselItem(
    val title: TextReference,
    val subtitle: TextReference,
)