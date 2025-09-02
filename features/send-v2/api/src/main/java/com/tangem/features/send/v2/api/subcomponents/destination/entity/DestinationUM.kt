package com.tangem.features.send.v2.api.subcomponents.destination.entity

import androidx.compose.runtime.Immutable
import kotlinx.collections.immutable.ImmutableList

@Immutable
sealed class DestinationUM {

    abstract val isPrimaryButtonEnabled: Boolean

    data class Content(
        override val isPrimaryButtonEnabled: Boolean,
        val addressTextField: DestinationTextFieldUM.RecipientAddress,
        val memoTextField: DestinationTextFieldUM.RecipientMemo?,
        val recent: ImmutableList<DestinationRecipientListUM>,
        val wallets: ImmutableList<DestinationRecipientListUM>,
        val networkName: String,
        val isValidating: Boolean = false,
        val isInitialized: Boolean = false,
        val isRedesignEnabled: Boolean = false,
        val isRecentHidden: Boolean,
    ) : DestinationUM()

    data class Empty(
        override val isPrimaryButtonEnabled: Boolean = false,
    ) : DestinationUM()
}