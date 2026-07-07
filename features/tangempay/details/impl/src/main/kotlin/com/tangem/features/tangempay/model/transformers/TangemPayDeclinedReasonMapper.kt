package com.tangem.features.tangempay.model.transformers

import com.tangem.core.ui.extensions.TextReference
import com.tangem.core.ui.extensions.resourceReference
import com.tangem.core.ui.extensions.stringReference
import com.tangem.features.tangempay.details.impl.R

/**
 * Maps a raw decline reason coming from the transaction metadata to a localized [TextReference].
 */
internal object TangemPayDeclinedReasonMapper {

    private val reasonToResId: Map<String, Int> = mapOf(
        "account credit limit exceeded" to R.string.tangempay_declined_reason_1,
        "automatic fuel dispenser velocity limit reached, more than 2 transactions were attempted within a 3-day " +
            "period" to R.string.tangempay_declined_reason_2,
        "block transaction from high-risk merchant category codes" to R.string.tangempay_declined_reason_3,
        "block transaction from restricted countries [v3-correlation]" to R.string.tangempay_declined_reason_4,
        "block transaction from specified high-risk e-commerce merchants" to R.string.tangempay_declined_reason_5,
        "block transactions from specified high-risk e-commerce merchants" to R.string.tangempay_declined_reason_5,
        "block transactions from specified high ecom merchant" to R.string.tangempay_declined_reason_5,
        "block transactions over 150 usd at automated fuel dispensers" to R.string.tangempay_declined_reason_6,
        "blocked mcc" to R.string.tangempay_declined_reason_7,
        "blocked merchant" to R.string.tangempay_declined_reason_8,
        "card locked" to R.string.tangempay_declined_reason_9,
        "card spending limit exceeded" to R.string.tangempay_declined_reason_10,
        "cvv2 match fail" to R.string.tangempay_declined_reason_11,
        "expiry in de14 not matching database stored expiry for this card" to R.string.tangempay_declined_reason_12,
        "incorrect pin" to R.string.tangempay_declined_reason_13,
        "transaction not permitted to cardholder" to R.string.tangempay_declined_reason_14,
        "transaction velocity limit reached, more than 25 transactions were attempted within a 2-day period" to
            R.string.tangempay_declined_reason_15,
        "triggers if there is a suspected bin attack from a merchant" to R.string.tangempay_declined_reason_16,
        "webhook declined" to R.string.tangempay_declined_reason_17,
    )

    fun map(declinedReason: String): TextReference {
        val resId = reasonToResId[declinedReason.trim().lowercase()]
        return if (resId != null) {
            resourceReference(resId)
        } else {
            stringReference(declinedReason)
        }
    }
}