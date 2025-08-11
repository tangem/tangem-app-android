package com.tangem.features.send.v2.api.subcomponents.destination.entity

import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.runtime.Immutable
import com.tangem.core.ui.extensions.TextReference
import com.tangem.utils.toBriefAddressFormat

@Immutable
sealed class DestinationTextFieldUM {

    /** Current value */
    abstract val value: String

    /** Keyboard options */
    abstract val keyboardOptions: KeyboardOptions

    data class RecipientAddress(
        override val value: String,
        override val keyboardOptions: KeyboardOptions,
        val placeholder: TextReference,
        val label: TextReference,
        val isError: Boolean = false,
        val error: TextReference? = null,
        val isValuePasted: Boolean,
        // if value is human-readable address, this field contains the actual blockchain address
        val blockchainAddress: String? = null,
    ) : DestinationTextFieldUM() {

        val actualAddress: String
            get() = blockchainAddress ?: value

        // if value is human-readable address, this field contains the actual brief blockchain address
        val briefBlockchainAddress: String?
            get() = blockchainAddress?.toBriefAddressFormat(BRIEF_ADDRESS_EDGE_LENGTH, BRIEF_ADDRESS_EDGE_LENGTH)
    }

    data class RecipientMemo(
        override val value: String,
        override val keyboardOptions: KeyboardOptions,
        val placeholder: TextReference,
        val label: TextReference,
        val isError: Boolean = false,
        val error: TextReference? = null,
        val disabledText: TextReference,
        val isEnabled: Boolean,
        val isValuePasted: Boolean,
    ) : DestinationTextFieldUM()

    private companion object {
        const val BRIEF_ADDRESS_EDGE_LENGTH = 13
    }
}