package com.tangem.features.promobanners.impl.model

internal data class PromoBannerDisplay(
    val id: Int,
    val placeholder: String,
    val priority: PromoBannerPriority,
    val title: String,
    val subtitle: String,
    val iconUrl: String?,
    val deeplink: String?,
    val isButtonEnabled: Boolean,
    val buttonText: String?,
    val isDismissable: Boolean,
)