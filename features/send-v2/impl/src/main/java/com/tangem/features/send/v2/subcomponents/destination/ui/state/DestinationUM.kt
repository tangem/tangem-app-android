package com.tangem.features.send.v2.subcomponents.destination.ui.state

import androidx.compose.runtime.Immutable
import kotlinx.collections.immutable.ImmutableList

@Immutable
internal sealed class DestinationUM {

    abstract val isPrimaryButtonEnabled: Boolean

    data class Content(
        override val isPrimaryButtonEnabled: Boolean,
        val addressTextField: DestinationTextFieldUM.RecipientAddress,
        val memoTextField: DestinationTextFieldUM.RecipientMemo?,
        val recent: ImmutableList<DestinationRecipientListUM>,
        val wallets: ImmutableList<DestinationRecipientListUM>,
        val networkName: String,
        val isValidating: Boolean = false,
        val isEditingDisabled: Boolean = false,
    ) : DestinationUM()

    data class Empty(
        override val isPrimaryButtonEnabled: Boolean = false,
    ) : DestinationUM()
}