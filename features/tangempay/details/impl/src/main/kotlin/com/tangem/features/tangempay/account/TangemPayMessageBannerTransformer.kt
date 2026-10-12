package com.tangem.features.tangempay.account

import com.tangem.utils.transformer.Transformer

internal class TangemPayMessageBannerTransformer(
    private val shouldShowProgress: Boolean,
) : Transformer<TangemPayDetailsUM> {

    override fun transform(prevState: TangemPayDetailsUM): TangemPayDetailsUM {
        val banner = prevState.messageBanner ?: return prevState
        val button = banner.state.secondaryButton ?: return prevState

        return prevState.copy(
            messageBanner = banner.copy(
                state = banner.state.copy(secondaryButton = button.copy(isLoading = shouldShowProgress)),
            ),
        )
    }
}