package com.tangem.features.tangempay.model.transformers

import com.tangem.core.ui.extensions.TextReference
import com.tangem.domain.pay.model.TangemPayCardDetails
import com.tangem.features.tangempay.details.impl.R
import com.tangem.features.tangempay.entity.TangemPayCardDetailsUM
import com.tangem.features.tangempay.entity.TangemPayDetailsUM
import com.tangem.features.tangempay.utils.CardDetailsFormatUtil
import com.tangem.utils.transformer.Transformer

internal class DetailsRevealedStateTransformer(
    private val details: TangemPayCardDetails,
    private val onClickHide: (() -> Unit),
) : Transformer<TangemPayDetailsUM> {

    override fun transform(prevState: TangemPayDetailsUM): TangemPayDetailsUM {
        val cardDetailsUM = TangemPayCardDetailsUM(
            number = CardDetailsFormatUtil.formatCardNumber(cardNumber = details.pan),
            expiry = CardDetailsFormatUtil.formatDate(month = details.expirationMonth, year = details.expirationYear),
            cvv = details.cvv,
            onClick = onClickHide,
            buttonText = TextReference.Res(R.string.tangempay_card_details_hide_text),
            onCopy = prevState.cardDetailsUM.onCopy,
            isHidden = false,
        )
        return prevState.copy(cardDetailsUM = cardDetailsUM)
    }
}