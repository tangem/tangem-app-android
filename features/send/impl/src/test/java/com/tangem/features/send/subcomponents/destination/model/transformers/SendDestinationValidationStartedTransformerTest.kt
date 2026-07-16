package com.tangem.features.send.subcomponents.destination.model.transformers

import androidx.compose.foundation.text.KeyboardOptions
import com.google.common.truth.Truth.assertThat
import com.tangem.core.ui.extensions.TextReference
import com.tangem.features.send.api.subcomponents.destination.entity.DestinationTextFieldUM
import com.tangem.features.send.api.subcomponents.destination.entity.DestinationUM
import kotlinx.collections.immutable.persistentListOf
import org.junit.jupiter.api.Test

class SendDestinationValidationStartedTransformerTest {

    @Test
    fun `GIVEN previously enabled button WHEN validation started THEN validating and button disabled`() {
        // Arrange — button was enabled by a prior successful validation
        val state = contentState(isPrimaryButtonEnabled = true)

        // Act — a new validation begins for the edited value
        val result = SendDestinationValidationStartedTransformer.transform(state) as DestinationUM.Content

        // Assert — button cannot be pressed with the stale enablement while validation is pending
        assertThat(result.isValidating).isTrue()
        assertThat(result.isPrimaryButtonEnabled).isFalse()
    }

    @Test
    fun `GIVEN empty state WHEN transform THEN state unchanged`() {
        val state = DestinationUM.Empty()

        val result = SendDestinationValidationStartedTransformer.transform(state)

        assertThat(result).isEqualTo(state)
    }

    private fun contentState(isPrimaryButtonEnabled: Boolean) = DestinationUM.Content(
        isPrimaryButtonEnabled = isPrimaryButtonEnabled,
        addressTextField = DestinationTextFieldUM.RecipientAddress(
            value = "r2.eth",
            keyboardOptions = KeyboardOptions.Default,
            placeholder = TextReference.EMPTY,
            label = TextReference.EMPTY,
            isValuePasted = false,
        ),
        memoTextField = null,
        recent = persistentListOf(),
        wallets = persistentListOf(),
        networkName = "Ethereum",
        isRecentHidden = false,
    )
}