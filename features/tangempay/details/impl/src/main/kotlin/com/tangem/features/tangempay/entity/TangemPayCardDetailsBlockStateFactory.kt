package com.tangem.features.tangempay.entity

import com.tangem.core.ui.extensions.resourceReference
import com.tangem.domain.visa.model.TangemPayCardFrozenState
import com.tangem.features.tangempay.details.impl.R
import com.tangem.utils.StringsSigns

internal class TangemPayCardDetailsBlockStateFactory(
    private val cardNumberEnd: String,
    private val onReveal: () -> Unit,
    private val onCopy: (String) -> Unit,
) {

    fun getInitialState() = TangemPayCardDetailsUM(
        number = "",
        numberShort = "${StringsSigns.ASTERISK}$cardNumberEnd",
        expiry = "",
        cvv = "",
        buttonText = resourceReference(R.string.tangempay_card_details_reveal_text),
        onClick = onReveal,
        onCopy = onCopy,
        isHidden = true,
        cardFrozenState = TangemPayCardFrozenState.Unfrozen,
    )
}