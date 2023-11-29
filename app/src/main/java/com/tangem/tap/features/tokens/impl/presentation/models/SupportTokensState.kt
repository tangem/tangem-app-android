package com.tangem.tap.features.tokens.impl.presentation.models

/**
 * State that shows is token support by given card
 */
sealed class SupportTokensState {

    object NetworkTokensUnsupported : SupportTokensState()
    object UnsupportedCurve : SupportTokensState()
    object SupportedToken : SupportTokensState()
}
