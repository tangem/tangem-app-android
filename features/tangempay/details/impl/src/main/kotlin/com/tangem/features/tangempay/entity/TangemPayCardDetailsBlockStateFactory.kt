package com.tangem.features.tangempay.entity

import com.tangem.core.ui.extensions.resourceReference
import com.tangem.domain.models.account.CardDisplayName
import com.tangem.domain.visa.model.TangemPayCardFrozenState
import com.tangem.features.tangempay.details.impl.R
import com.tangem.features.tangempay.model.CardDataType
import com.tangem.utils.StringsSigns

internal class TangemPayCardDetailsBlockStateFactory(
    private val cardNumberEnd: String,
    private val displayName: CardDisplayName?,
    private val isEditingNameEnabled: Boolean,
    private val onEditNameClick: () -> Unit,
    private val onReveal: () -> Unit,
    private val onCopy: (String, CardDataType) -> Unit,
) {

    fun getInitialState(): TangemPayCardDetailsUM {
        return TangemPayCardDetailsUM(
            number = "",
            numberShort = "${StringsSigns.ASTERISK}$cardNumberEnd",
            expiry = "",
            cvv = "",
            buttonText = resourceReference(R.string.tangempay_card_details_reveal_text),
            onClick = onReveal,
            onCopy = onCopy,
            isHidden = true,
            cardFrozenState = TangemPayCardFrozenState.Unfrozen,
            displayNameState = if (displayName != null) {
                DisplayNameState.Display(
                    displayName = displayName.value,
                    onClick = onEditNameClick,
                    isEditingEnabled = isEditingNameEnabled,
                )
            } else {
                null
            },
        )
    }
}