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
        title = stringReference("address"),
        subtitle = stringReference("0.001 BTC"),
        timestamp = stringReference("1.01.1970, 00:00"),
        subtitleEndOffset = 0,
        subtitleIconRes = R.drawable.ic_arrow_down_24,
        isVisible = true,
        isLoading = false,
    )

    val recipientState = SendStates.RecipientState(
        addressTextField = SendTextField.RecipientAddress(
            value = "0x23948239805671983476598176",
            onValueChange = {},
            keyboardOptions = KeyboardOptions.Default,
            placeholder = stringReference("Placeholder"),
            label = stringReference("Recipient"),
            isError = false,
            error = null,
            isValuePasted = false,
        ),
        memoTextField = null,
        recent = persistentListOf(
            defaultRecentItem.copy(id = "1"),
            defaultRecentItem.copy(id = "2"),
            defaultRecentItem.copy(id = "3"),
        ),
        wallets = persistentListOf(
            defaultRecentItem.copy(id = "4", subtitle = stringReference("Wallet")),
        ),
        network = "Ethereum",
        isValidating = false,
        isPrimaryButtonEnabled = true,
    )
}