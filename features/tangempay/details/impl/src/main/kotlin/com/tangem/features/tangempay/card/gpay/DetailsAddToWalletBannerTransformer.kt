package com.tangem.features.tangempay.card.gpay

import com.tangem.features.tangempay.account.TangemPayDetailsUM
import com.tangem.utils.transformer.Transformer

internal class DetailsAddToWalletBannerTransformer(
    private val onClickBanner: () -> Unit,
    private val onClickCloseBanner: () -> Unit,
    private val isDone: Boolean,
) : Transformer<TangemPayDetailsUM> {

    override fun transform(prevState: TangemPayDetailsUM): TangemPayDetailsUM {
        return prevState.copy(
            addToWalletBlockState = if (isDone) {
                null
            } else {
                AddToWalletBlockState(
                    onClick = onClickBanner,
                    onClickClose = onClickCloseBanner,
                )
            },
        )
    }
}