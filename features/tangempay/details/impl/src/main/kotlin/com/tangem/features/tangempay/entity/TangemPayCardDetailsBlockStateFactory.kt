package com.tangem.features.tangempay.entity

import com.tangem.core.ui.extensions.resourceReference
import com.tangem.domain.models.account.CardDisplayName
import com.tangem.domain.models.pay.TangemPayCardFrozenState
import com.tangem.features.tangempay.details.impl.R
import com.tangem.utils.StringsSigns

@Suppress("LongParameterList")
internal class TangemPayCardDetailsBlockStateFactory(
    private val cardNumberEnd: String,
    private val displayName: CardDisplayName?,
    private val isEditingNameEnabled: Boolean,
    private val onEditNameClick: () -> Unit,
    private val onReveal: () -> Unit,
    private val onCopy: (String, CardDataType) -> Unit,
    private val shouldShowCardDetailsButtonOnCard: Boolean,
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
            cardFrozenState = TangemPayCardFrozenState.Pending,
            displayNameState = if (displayName != null) {
                DisplayNameState.Display(
                    displayName = displayName.value,
                    onClick = onEditNameClick,
                    isEditingEnabled = isEditingNameEnabled,
                )
            } else {
                null
            },
            shouldShowCardDetailsButtonOnCard = shouldShowCardDetailsButtonOnCard,
        )
    }
}