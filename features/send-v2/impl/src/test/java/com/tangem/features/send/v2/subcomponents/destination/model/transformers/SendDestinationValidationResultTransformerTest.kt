package com.tangem.features.send.v2.subcomponents.destination.model.transformers

import androidx.compose.foundation.text.KeyboardOptions
import arrow.core.Either
import arrow.core.left
import arrow.core.right
import com.google.common.truth.Truth.assertThat
import com.tangem.core.ui.extensions.TextReference
import com.tangem.core.ui.extensions.resourceReference
import com.tangem.domain.transaction.error.AddressValidation
import com.tangem.domain.transaction.error.AddressValidationResult
import com.tangem.domain.transaction.error.ValidateMemoError
import com.tangem.features.send.v2.api.subcomponents.destination.entity.DestinationTextFieldUM
import com.tangem.features.send.v2.api.subcomponents.destination.entity.DestinationUM
import com.tangem.features.send.v2.impl.R
import kotlinx.collections.immutable.persistentListOf
import org.junit.jupiter.api.Test

class SendDestinationValidationResultTransformerTest {

    private val validAddress: AddressValidationResult = AddressValidation.Success.Valid.right()
    private val invalidAddress: AddressValidationResult = AddressValidation.Error.InvalidAddress.left()
    private val xAddress: AddressValidationResult = AddressValidation.Success.ValidXAddress.right()

    private val validMemo = Unit.right()
    private val invalidMemo = ValidateMemoError.InvalidMemo.left()

    private val formatErrorRef = resourceReference(R.string.send_memo_destination_tag_error)
    private val tagRequiredRef = resourceReference(R.string.send_validation_destination_tag_required_title)

    @Test
    fun `GIVEN required memo is empty WHEN transform THEN tag required error shown and primary button disabled`() {
        val result = transform(validAddress, validMemo, isMemoRequired = true, memo = "")

        assertThat(result.isPrimaryButtonEnabled).isFalse()
        assertThat(result.memoTextField?.isError).isTrue()
        assertThat(result.memoTextField?.error).isEqualTo(tagRequiredRef)
    }

    @Test
    fun `GIVEN required memo is filled with valid value WHEN transform THEN primary button enabled`() {
        val result = transform(validAddress, validMemo, isMemoRequired = true, memo = "123")

        assertThat(result.isPrimaryButtonEnabled).isTrue()
        assertThat(result.memoTextField?.isError).isFalse()
    }

    @Test
    fun `GIVEN required memo filled with whitespace only WHEN transform THEN tag required error shown and primary button disabled`() {
        val result = transform(validAddress, validMemo, isMemoRequired = true, memo = "   ")

        assertThat(result.isPrimaryButtonEnabled).isFalse()
        assertThat(result.memoTextField?.isError).isTrue()
        assertThat(result.memoTextField?.error).isEqualTo(tagRequiredRef)
    }

    @Test
    fun `GIVEN empty memo that is not required WHEN transform THEN field valid and primary button enabled`() {
        val result = transform(validAddress, validMemo, isMemoRequired = false, memo = "")

        assertThat(result.isPrimaryButtonEnabled).isTrue()
        assertThat(result.memoTextField?.isError).isFalse()
    }

    @Test
    fun `GIVEN entered memo with valid format WHEN transform THEN primary button enabled`() {
        val result = transform(validAddress, validMemo, isMemoRequired = false, memo = "valid-memo")

        assertThat(result.isPrimaryButtonEnabled).isTrue()
        assertThat(result.memoTextField?.isError).isFalse()
    }

    @Test
    fun `GIVEN entered memo with invalid format WHEN transform THEN format error shown and primary button disabled`() {
        val result = transform(validAddress, invalidMemo, isMemoRequired = false, memo = "bad-memo")

        assertThat(result.isPrimaryButtonEnabled).isFalse()
        assertThat(result.memoTextField?.isError).isTrue()
        assertThat(result.memoTextField?.error).isEqualTo(formatErrorRef)
    }

    @Test
    fun `GIVEN invalid memo after prior tag required state WHEN transform THEN format error shown`() {
        val staleState = contentState(memo = "bad-memo").let { state ->
            state.copy(memoTextField = state.memoTextField?.copy(error = tagRequiredRef))
        }

        val result = SendDestinationValidationResultTransformer(
            addressValidationResult = validAddress,
            memoValidationResult = invalidMemo,
            isMemoRequired = false,
        ).transform(staleState) as DestinationUM.Content

        assertThat(result.memoTextField?.isError).isTrue()
        assertThat(result.memoTextField?.error).isEqualTo(formatErrorRef)
    }

    @Test
    fun `GIVEN invalid address WHEN transform THEN primary button disabled`() {
        val result = transform(invalidAddress, validMemo, isMemoRequired = false, memo = "123")

        assertThat(result.isPrimaryButtonEnabled).isFalse()
    }

    @Test
    fun `GIVEN x-address with required memo WHEN transform THEN memo field disabled cleared and primary button enabled`() {
        val result = transform(xAddress, validMemo, isMemoRequired = true, memo = "")

        assertThat(result.memoTextField?.isEnabled).isFalse()
        assertThat(result.memoTextField?.isError).isFalse()
        assertThat(result.memoTextField?.value).isEmpty()
        assertThat(result.isPrimaryButtonEnabled).isTrue()
    }

    @Test
    fun `GIVEN x-address with stale invalid memo WHEN transform THEN memo ignored field disabled and primary button enabled`() {
        val result = transform(xAddress, invalidMemo, isMemoRequired = false, memo = "stale-memo")

        assertThat(result.memoTextField?.isEnabled).isFalse()
        assertThat(result.memoTextField?.isError).isFalse()
        assertThat(result.memoTextField?.value).isEmpty()
        assertThat(result.isPrimaryButtonEnabled).isTrue()
    }

    @Test
    fun `GIVEN network without memo field WHEN transform THEN memo field is null and primary button enabled`() {
        val state = contentState(memo = "").copy(memoTextField = null)

        val result = SendDestinationValidationResultTransformer(
            addressValidationResult = validAddress,
            memoValidationResult = validMemo,
            isMemoRequired = false,
        ).transform(state) as DestinationUM.Content

        assertThat(result.memoTextField).isNull()
        assertThat(result.isPrimaryButtonEnabled).isTrue()
    }

    private fun transform(
        address: AddressValidationResult,
        memoResult: Either<ValidateMemoError, Unit>,
        isMemoRequired: Boolean,
        memo: String,
    ): DestinationUM.Content = SendDestinationValidationResultTransformer(
        addressValidationResult = address,
        memoValidationResult = memoResult,
        isMemoRequired = isMemoRequired,
    ).transform(contentState(memo = memo)) as DestinationUM.Content

    private fun contentState(memo: String) = DestinationUM.Content(
        isPrimaryButtonEnabled = false,
        addressTextField = DestinationTextFieldUM.RecipientAddress(
            value = "0xRecipient",
            keyboardOptions = KeyboardOptions.Default,
            placeholder = TextReference.EMPTY,
            label = TextReference.EMPTY,
            isValuePasted = false,
        ),
        memoTextField = DestinationTextFieldUM.RecipientMemo(
            value = memo,
            keyboardOptions = KeyboardOptions.Default,
            placeholder = TextReference.EMPTY,
            label = TextReference.EMPTY,
            error = formatErrorRef,
            disabledText = TextReference.EMPTY,
            isEnabled = true,
            isValuePasted = false,
        ),
        recent = persistentListOf(),
        wallets = persistentListOf(),
        networkName = "Ethereum",
        isRecentHidden = false,
    )
}