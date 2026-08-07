package com.tangem.features.send.subcomponents.destination.model.transformers

import androidx.compose.foundation.text.KeyboardOptions
import com.google.common.truth.Truth.assertThat
import com.tangem.common.ui.account.AccountIconUM
import com.tangem.core.ui.extensions.TextReference
import com.tangem.features.send.api.subcomponents.destination.entity.DestinationTextFieldUM
import com.tangem.features.send.api.subcomponents.destination.entity.DestinationUM
import io.mockk.mockk
import kotlinx.collections.immutable.persistentListOf
import org.junit.jupiter.api.Test

class SendDestinationAddressTransformerTest {

    @Test
    fun `GIVEN previously resolved address WHEN recipient edited THEN blockchainAddress and contact reset`() {
        // Arrange — R1 was resolved to canonical A1 and recognized as a contact
        val state = contentState(
            value = "r1.eth",
            blockchainAddress = "0xCanonicalAddressA1",
            contactName = "Alice",
            contactIcon = mockk(),
        )

        // Act — user replaces the recipient with a new, not-yet-validated name R2
        val result = SendDestinationAddressTransformer(address = "r2.eth", isPasted = false)
            .transform(state) as DestinationUM.Content

        // Assert — the new value is shown and all previously resolved/recognized data is cleared,
        // so actualAddress falls back to the raw R2 instead of the stale canonical A1
        val addressField = result.addressTextField
        assertThat(addressField.value).isEqualTo("r2.eth")
        assertThat(addressField.blockchainAddress).isNull()
        assertThat(addressField.contactName).isNull()
        assertThat(addressField.contactIcon).isNull()
        assertThat(addressField.actualAddress).isEqualTo("r2.eth")
        assertThat(addressField.isValuePasted).isFalse()
    }

    @Test
    fun `GIVEN pasted value WHEN recipient edited THEN isValuePasted propagated`() {
        val state = contentState(value = "")

        val result = SendDestinationAddressTransformer(address = "0xPasted", isPasted = true)
            .transform(state) as DestinationUM.Content

        assertThat(result.addressTextField.value).isEqualTo("0xPasted")
        assertThat(result.addressTextField.isValuePasted).isTrue()
    }

    @Test
    fun `GIVEN empty state WHEN transform THEN state unchanged`() {
        val state = DestinationUM.Empty()

        val result = SendDestinationAddressTransformer(address = "r2.eth", isPasted = false).transform(state)

        assertThat(result).isEqualTo(state)
    }

    private fun contentState(
        value: String,
        blockchainAddress: String? = null,
        contactName: String? = null,
        contactIcon: AccountIconUM.CryptoPortfolio? = null,
    ) = DestinationUM.Content(
        isPrimaryButtonEnabled = false,
        addressTextField = DestinationTextFieldUM.RecipientAddress(
            value = value,
            keyboardOptions = KeyboardOptions.Default,
            placeholder = TextReference.EMPTY,
            label = TextReference.EMPTY,
            isValuePasted = false,
            blockchainAddress = blockchainAddress,
            contactName = contactName,
            contactIcon = contactIcon,
        ),
        memoTextField = null,
        recent = persistentListOf(),
        wallets = persistentListOf(),
        networkName = "Ethereum",
        isRecentHidden = false,
    )
}