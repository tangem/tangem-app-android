package com.tangem.features.tangempay.entity

import com.tangem.core.ui.extensions.TextReference
import com.tangem.features.tangempay.details.impl.R
import com.tangem.features.tangempay.utils.CardDetailsFormatUtil
import com.tangem.utils.StringsSigns

private const val CARD_NUMBER_SART_DIGITS_COUNT = 12
private const val DATE_PART_LENGTH = 2
private const val CVV_LENGTH = 3

internal class TangemPayCardDetailsBlockStateFactory(
    private val cardNumberEnd: String,
    private val onReveal: () -> Unit,
    private val onCopy: (String) -> Unit,
) {

    private val cardStartMasked = maskedBlock(CARD_NUMBER_SART_DIGITS_COUNT)
    private val dateMasked = maskedBlock(DATE_PART_LENGTH)
    private val cvvMasked = maskedBlock(CVV_LENGTH)

    fun getInitialState() = TangemPayCardDetailsUM(
        number = CardDetailsFormatUtil.formatCardNumber(cardNumber = "$cardStartMasked$cardNumberEnd"),
        expiry = CardDetailsFormatUtil.formatDate(month = dateMasked, year = dateMasked),
        cvv = cvvMasked,
        buttonText = TextReference.Res(R.string.tangempay_card_details_reveal_text),
        onClick = onReveal,
        onCopy = onCopy,
        isHidden = true,
    )

    private fun maskedBlock(count: Int) = StringsSigns.DOT.repeat(count)
}