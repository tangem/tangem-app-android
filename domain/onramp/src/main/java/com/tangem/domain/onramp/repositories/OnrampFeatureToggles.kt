package com.tangem.domain.onramp.repositories

interface OnrampFeatureToggles {

    val isThemedPaymentMethodImagesEnabled: Boolean

    val isExpressCategoriesGeoBlockingEnabled: Boolean
}