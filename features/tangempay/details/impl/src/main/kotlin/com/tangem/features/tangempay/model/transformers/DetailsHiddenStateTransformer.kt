package com.tangem.features.tangempay.model.transformers

import com.tangem.core.ui.extensions.TextReference
import com.tangem.features.tangempay.details.impl.R
import com.tangem.features.tangempay.entity.TangemPayCardDetailsUM
import com.tangem.features.tangempay.entity.TangemPayDetailsUM
import com.tangem.features.tangempay.utils.CardDetailsFormatUtil
import com.tangem.utils.StringsSigns
import com.tangem.utils.transformer.Transformer

private const val CARD_NUMBER_SART_DIGITS_COUNT = 12
private const val DATE_PART_LENGTH = 2
private const val CVV_LENGTH = 3

internal class DetailsHiddenStateTransformer(
    private val onClickReveal: () -> Unit,
    private val cardNumberEnd: String,
) : Transformer<TangemPayDetailsUM> {

    private val cardStartMasked = maskedBlock(CARD_NUMBER_SART_DIGITS_COUNT)
    private val dateMasked = maskedBlock(DATE_PART_LENGTH)
    private val cvvMasked = maskedBlock(CVV_LENGTH)

    override fun transform(prevState: TangemPayDetailsUM): TangemPayDetailsUM {
        val cardDetailsUM = TangemPayCardDetailsUM(
            number = CardDetailsFormatUtil.formatCardNumber(cardNumber = "$cardStartMasked$cardNumberEnd"),
            expiry = CardDetailsFormatUtil.formatDate(month = dateMasked, year = dateMasked),
            cvv = cvvMasked,
            buttonText = TextReference.Res(R.string.tangempay_card_details_reveal_text),
            onClick = onClickReveal,
            onCopy = {},
            isHidden = true,
        )
        return prevState.copy(cardDetailsUM = cardDetailsUM)
    }

    private fun maskedBlock(count: Int) = StringsSigns.DOT.repeat(count)
}