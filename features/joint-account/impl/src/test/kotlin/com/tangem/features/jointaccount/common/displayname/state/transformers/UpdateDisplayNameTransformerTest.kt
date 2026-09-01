package com.tangem.features.jointaccount.common.displayname.state.transformers

import com.google.common.truth.Truth.assertThat
import com.tangem.core.ui.extensions.stringReference
import com.tangem.features.jointaccount.common.displayname.ui.state.JointAccountDisplayNameUM
import com.tangem.test.core.ProvideTestModels
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.params.ParameterizedTest

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class UpdateDisplayNameTransformerTest {

    private val prevState = createState(name = "Old", isButtonEnabled = true)

    @ParameterizedTest
    @ProvideTestModels
    fun transform(model: TransformModel) {
        // Act
        val actual = UpdateDisplayNameTransformer(name = model.input).transform(prevState)

        // Assert
        val expected = createState(
            name = model.expectedName,
            isButtonEnabled = model.expectedButtonEnabled,
        )
        assertThat(actual).isEqualTo(expected)
    }

    private fun provideTestModels() = listOf(
        // typed input within the limit is applied as is
        TransformModel(
            input = "Ivan Zolo",
            expectedName = "Ivan Zolo", expectedButtonEnabled = true,
        ),
        // letters of any script and digits are allowed
        TransformModel(
            input = "Иван 2й",
            expectedName = "Иван 2й", expectedButtonEnabled = true,
        ),
        // emoji are allowed
        TransformModel(
            input = "👨‍👩‍👧 👍🏽",
            expectedName = "👨‍👩‍👧 👍🏽", expectedButtonEnabled = true,
        ),
        // real names carry hyphens, apostrophes and diacritics
        TransformModel(
            input = "Anna-Maria O'Neil José",
            expectedName = "Anna-Maria O'Neil José", expectedButtonEnabled = true,
        ),
        // any other special characters are allowed too
        TransformModel(
            input = "Ivan!",
            expectedName = "Ivan!", expectedButtonEnabled = true,
        ),
        // exactly 25 characters is the maximum allowed
        TransformModel(
            input = "a".repeat(n = 25),
            expectedName = "a".repeat(n = 25), expectedButtonEnabled = true,
        ),
        // the 26th character is not entered: the whole change is rejected, the previous value stays
        TransformModel(
            input = "a".repeat(n = 26),
            expectedName = "Old", expectedButtonEnabled = true,
        ),
        // an over-long paste is rejected entirely, not truncated
        TransformModel(
            input = "a".repeat(n = 40),
            expectedName = "Old", expectedButtonEnabled = true,
        ),
        // the name is required: clearing the field disables the button
        TransformModel(
            input = "",
            expectedName = "", expectedButtonEnabled = false,
        ),
        // spaces are allowed characters, but a blank-only name is not a name
        TransformModel(
            input = "   ",
            expectedName = "   ", expectedButtonEnabled = false,
        ),
    )

    internal data class TransformModel(
        val input: String,
        val expectedName: String,
        val expectedButtonEnabled: Boolean,
    )

    private fun createState(name: String, isButtonEnabled: Boolean): JointAccountDisplayNameUM {
        return JointAccountDisplayNameUM(
            name = name,
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