package com.tangem.features.send.impl.presentation.state.previewdata

import androidx.compose.foundation.text.KeyboardOptions
import com.tangem.core.ui.extensions.stringReference
import com.tangem.features.send.impl.R
import com.tangem.features.send.impl.presentation.domain.SendRecipientListContent
import com.tangem.features.send.impl.presentation.state.SendStates
import com.tangem.features.send.impl.presentation.state.fields.SendTextField
import kotlinx.collections.immutable.persistentListOf

internal object RecipientStatePreviewData {

    private val defaultRecentItem = SendRecipientListContent(
        id = "sanctus",
        title = stringReference("0x391316a070212312312378E88CAc8A0C250"),
        subtitleEndOffset = 0,
        subtitleIconRes = R.drawable.ic_arrow_down_24,
        isVisible = true,
        isLoading = false,
    )

    val recipientState = SendStates.RecipientState(
        addressTextField = SendTextField.RecipientAddress(
            value = "",
            onValueChange = {},
            keyboardOptions = KeyboardOptions.Default,
            placeholder = stringReference("Enter address"),
            label = stringReference("Recipient"),
            isError = false,
            error = null,
            isValuePasted = false,
        ),
        memoTextField = SendTextField.RecipientMemo(
            value = "",
            onValueChange = {},
            keyboardOptions = KeyboardOptions.Default,
            placeholder = stringReference("Optional"),
            label = stringReference("Memo"),
            isError = false,
            error = null,
            disabledText = stringReference("Already included in the entered address"),
            isEnabled = true,
            isValuePasted = false,
        ),
        recent = persistentListOf(),
        wallets = persistentListOf(),
        network = "Ethereum",
        isValidating = false,
        isPrimaryButtonEnabled = true,
    )

    val recipientAddressState = recipientState.copy(
        addressTextField = recipientState.addressTextField.copy(
            value = "0x391316d97a07027a0702c8A002c8A0C25d8470",
        ),
    )

    val recipientWithRecentState = recipientState.copy(
        recent = persistentListOf(
            defaultRecentItem.copy(
                id = "1",
                subtitle = stringReference("1 000 000 000.0004 USDT"),
                timestamp = stringReference("today at 14:46"),
                subtitleIconRes = R.drawable.ic_arrow_up_24,
            ),
            defaultRecentItem.copy(
                id = "2",
                subtitle = stringReference("20,09 USDT"),
                timestamp = stringReference("24.05.2004 at 14:46"),
            ),
            defaultRecentItem.copy(
                id = "3",
                subtitle = stringReference("20,09 USDT"),
                timestamp = stringReference("24.05.2004 at 14:46"),
            ),
        ),
        wallets = persistentListOf(
            defaultRecentItem.copy(
                id = "4",
                subtitle = stringReference("Main Wallet"),
                subtitleIconRes = null,
            ),
        ),
    )
}