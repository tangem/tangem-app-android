package com.tangem.features.polymarket.impl.placeprediction.entity

import com.tangem.features.polymarket.impl.placeprediction.PlacePredictionComponent

/** The conditions accepted on Amount, shared by the summary display and the signing request. */
internal data class PreparedPrediction(
    val params: PlacePredictionComponent.Params,
    val state: PlacePredictionUM,
    val isNegRisk: Boolean,
) {

    val quote: SignableQuoteUM = requireNotNull((state.quote as? QuoteUM.Content)?.signable)
    val isFullClose: Boolean = state.isFullClose()

    init {
        require(state.isPrimaryButtonEnabled)
    }
}