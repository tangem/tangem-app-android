package com.tangem.features.tangempay.model.transformers

import com.tangem.core.ui.extensions.resourceReference
import com.tangem.domain.pay.model.TangemPayCardDetails
import com.tangem.features.tangempay.details.impl.R
import com.tangem.features.tangempay.entity.TangemPayCardDetailsUM
import com.tangem.utils.transformer.Transformer

private const val CARD_NUMBER_CHUNK_LENGTH = 4
private const val DATE_LENGTH = 2

internal class DetailsRevealedStateTransformer(
    private val details: TangemPayCardDetails,
    private val onClickHide: (() -> Unit),
) : Transformer<TangemPayCardDetailsUM> {

    override fun transform(prevState: TangemPayCardDetailsUM): TangemPayCardDetailsUM {
        return prevState.copy(
            number = formatCardNumber(cardNumber = details.pan),
            expiry = formatDate(month = details.expirationMonth, year = details.expirationYear),
            cvv = details.cvv,
            onClick = onClickHide,
            buttonText = resourceReference(R.string.tangempay_card_details_hide_text),
            isHidden = false,
        )
    }

    private fun formatCardNumber(cardNumber: String): String {
        return cardNumber
            .filterNot { it.isWhitespace() }
            .chunked(CARD_NUMBER_CHUNK_LENGTH).joinToString(" ")
    }

    private fun formatDate(month: String, year: String): String {
        val monthFormatted = month.padStart(DATE_LENGTH, '0')
        val yearFormatted = year.takeLast(DATE_LENGTH)
        return "$monthFormatted/$yearFormatted"
    }
}