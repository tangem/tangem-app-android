package com.tangem.features.tangempay.card.view

import androidx.compose.runtime.Immutable
import androidx.compose.ui.text.input.TextFieldValue
import com.tangem.core.ui.extensions.TextReference
import com.tangem.domain.models.pay.TangemPayCardFrozenState
import com.tangem.domain.models.pay.TangemPayCardState

internal enum class CardDataType {
    Number, Expiry, CVV
}

internal data class TangemPayCardDetailsUM(
    val number: String,
    val numberShort: String,
    val expiry: String,
    val cvv: String,
    val buttonText: TextReference = TextReference.EMPTY,
    val onClick: () -> Unit = {},
    val onCopy: (String, CardDataType) -> Unit,
    val isHidden: Boolean = true,
    val isLoading: Boolean = false,
    val cardFrozenState: TangemPayCardFrozenState,
    val displayNameState: DisplayNameState?,
    val cardImageUrl: String?,
    val cardBackgroundImageUrl: String?,
    val isActionsAvailable: Boolean = false,
    val shouldShowCardDetailsButtonOnCard: Boolean = false,
    val cardState: TangemPayCardState = TangemPayCardState.Active,
)

@Immutable
internal sealed interface DisplayNameState {

    val displayName: String

    data class Display(
        override val displayName: String,
        val onClick: () -> Unit,
        val isEditingEnabled: Boolean,
    ) : DisplayNameState

    data class Editing(
        override val displayName: String,
        val editingValue: TextFieldValue,
        val isSubmitEnabled: Boolean,
        val onValueChanged: (TextFieldValue) -> Unit,
        val onSubmit: () -> Unit,
        val onDismiss: () -> Unit,
    ) : DisplayNameState

    fun copySealed(displayName: String): DisplayNameState {
        return when (this) {
            is Display -> copy(displayName = displayName)
            is Editing -> copy(displayName = displayName)
        }
    }
}