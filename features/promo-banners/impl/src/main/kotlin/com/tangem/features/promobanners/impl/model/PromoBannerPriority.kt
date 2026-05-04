package com.tangem.features.promobanners.impl.model

internal enum class PromoBannerPriority(val order: Int) {
    TOP(order = 0),
    HIGH(order = 1),
    MEDIUM(order = 2),
    LOW(order = 3),
}