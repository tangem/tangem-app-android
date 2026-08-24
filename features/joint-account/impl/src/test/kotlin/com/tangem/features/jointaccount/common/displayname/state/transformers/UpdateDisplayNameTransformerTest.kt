package com.tangem.features.jointaccount.common.displayname.state.transformers

import com.google.common.truth.Truth.assertThat
import com.tangem.core.ui.extensions.stringReference
import com.tangem.features.jointaccount.common.displayname.ui.state.JointAccountDisplayNameUM
import com.tangem.test.core.ProvideTestModels
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.params.ParameterizedTest

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class UpdateDisplayNameTransformerTest {

    private val prevState = createState(name = "Old", isError = false, isButtonEnabled = true)

    @ParameterizedTest
    @ProvideTestModels
    fun transform(model: TransformModel) {
        // Act
        val actual = UpdateDisplayNameTransformer(name = model.input).transform(prevState)

        // Assert
        val expected = createState(
            name = model.expectedName,
            isError = model.expectedError,
            isButtonEnabled = model.expectedButtonEnabled,
        )
        assertThat(actual).isEqualTo(expected)
    }

    private fun provideTestModels() = listOf(
        // typed input within the limit is applied as is
        TransformModel(
            input = "Ivan Zolo",
            expectedName = "Ivan Zolo", expectedError = false, expectedButtonEnabled = true,
        ),
        // letters of any script and digits are allowed
        TransformModel(
            input = "Иван 2й",
            expectedName = "Иван 2й", expectedError = false, expectedButtonEnabled = true,
        ),
        // plain pictograph emoji are allowed
        TransformModel(
            input = "Ivan 🚀",
            expectedName = "Ivan 🚀", expectedError = false, expectedButtonEnabled = true,
        ),
        // ZWJ sequences and skin-tone modifiers are allowed
        TransformModel(
            input = "👨‍👩‍👧 👍🏽",
            expectedName = "👨‍👩‍👧 👍🏽", expectedError = false, expectedButtonEnabled = true,
        ),
        // keycap emoji are allowed: digit + variation selector + combining keycap
        TransformModel(
            input = "Signer 1️⃣",
            expectedName = "Signer 1️⃣", expectedError = false, expectedButtonEnabled = true,
        ),
        // a special character stays visible but marks the field as an error
        TransformModel(
            input = "Ivan!",
            expectedName = "Ivan!", expectedError = true, expectedButtonEnabled = false,
        ),
        // punctuation counts as special characters too
        TransformModel(
            input = "Ivan-Zolo",
            expectedName = "Ivan-Zolo", expectedError = true, expectedButtonEnabled = false,
        ),
        // exactly 25 characters is the maximum allowed
        TransformModel(
            input = "a".repeat(n = 25),
            expectedName = "a".repeat(n = 25), expectedError = false, expectedButtonEnabled = true,
        ),
        // the 26th character is not entered: the whole change is rejected, the previous value stays
        TransformModel(
            input = "a".repeat(n = 26),
            expectedName = "Old", expectedError = false, expectedButtonEnabled = true,
        ),
        // an over-long paste is rejected entirely, not truncated
        TransformModel(
            input = "a".repeat(n = 40),
            expectedName = "Old", expectedError = false, expectedButtonEnabled = true,
        ),
        // the name is required: clearing the field disables the button without an error
        TransformModel(
            input = "",
            expectedName = "", expectedError = false, expectedButtonEnabled = false,
        ),
        // spaces are allowed characters, but a blank-only name is not a name
        TransformModel(
            input = "   ",
            expectedName = "   ", expectedError = false, expectedButtonEnabled = false,
        ),
    )

    internal data class TransformModel(
        val input: String,
        val expectedName: String,
        val expectedError: Boolean,
        val expectedButtonEnabled: Boolean,
    )

    private fun createState(name: String, isError: Boolean, isButtonEnabled: Boolean): JointAccountDisplayNameUM {
        return JointAccountDisplayNameUM(
            name = name,
            isError = isError,
            buttonText = buttonText,
            buttonIconRes = null,
            isButtonEnabled = isButtonEnabled,
            isButtonLoading = false,
            onNameChange = onNameChange,
            onContinueClick = onContinueClick,
            onBackClick = onBackClick,
        )
    }

    private companion object {
        // Shared instances so states built before and after a transform compare equal
        val buttonText = stringReference(value = "Create account")
        val onNameChange: (String) -> Unit = {}
        val onContinueClick: () -> Unit = {}
        val onBackClick: () -> Unit = {}
    }
}