package com.tangem.features.tangempay.model.transformers

import com.tangem.features.tangempay.ui.TangemPayOnboardingScreenState
import com.tangem.utils.transformer.Transformer

internal class TangemPayOnboardingButtonLoadingTransformer(
    private val isLoading: Boolean,
) : Transformer<TangemPayOnboardingScreenState> {
    override fun transform(prevState: TangemPayOnboardingScreenState): TangemPayOnboardingScreenState {
        val contentState = prevState as? TangemPayOnboardingScreenState.Content ?: return prevState
        val updatedButtonConfig = contentState.buttonConfig.copy(isLoading = isLoading)
        return contentState.copy(buttonConfig = updatedButtonConfig)
    }
}