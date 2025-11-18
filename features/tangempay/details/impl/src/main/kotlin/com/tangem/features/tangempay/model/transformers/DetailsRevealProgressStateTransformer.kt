package com.tangem.features.tangempay.model.transformers

import com.tangem.core.ui.extensions.TextReference
import com.tangem.features.tangempay.details.impl.R
import com.tangem.features.tangempay.entity.TangemPayDetailsUM
import com.tangem.utils.transformer.Transformer

internal class DetailsRevealProgressStateTransformer(
    private val onClickHide: (() -> Unit),
) : Transformer<TangemPayDetailsUM> {

    override fun transform(prevState: TangemPayDetailsUM): TangemPayDetailsUM {
        return prevState.copy(
            cardDetailsUM = prevState.cardDetailsUM.copy(
                buttonText = TextReference.Res(R.string.tangempay_card_details_hide_text),
                onClick = onClickHide,
                isLoading = true,
                isHidden = true,
            ),
        )
    }
}