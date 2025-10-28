package com.tangem.features.tangempay.model.transformers

import com.tangem.core.ui.extensions.TextReference
import com.tangem.features.tangempay.details.impl.R
import com.tangem.features.tangempay.entity.TangemPayCardDetailsUM
import com.tangem.utils.transformer.Transformer

internal class DetailsRevealProgressStateTransformer(
    private val onClickHide: (() -> Unit),
) : Transformer<TangemPayCardDetailsUM> {

    override fun transform(prevState: TangemPayCardDetailsUM): TangemPayCardDetailsUM {
        return prevState.copy(
            buttonText = TextReference.Res(R.string.tangempay_card_details_hide_text),
            onClick = onClickHide,
            isLoading = true,
            isHidden = true,
        )
    }
}