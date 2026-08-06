package com.tangem.features.commonfeatures.api.portfolioselector

interface PortfolioSelectorFeatureToggles {

    /**
     * Enables the V3 layout of the portfolio selector.
     *
     * Backed by the same TWI_1469_FOR_YOU_ENABLED toggle as the "For You" feature,
     * so both roll out together without a direct dependency between the features.
     */
    val isSelectorV3Enabled: Boolean
}