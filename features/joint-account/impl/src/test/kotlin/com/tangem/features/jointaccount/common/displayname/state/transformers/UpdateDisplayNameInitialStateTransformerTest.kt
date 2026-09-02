package com.tangem.features.jointaccount.common.displayname.state.transformers

import com.google.common.truth.Truth.assertThat
import com.tangem.core.ui.R
import com.tangem.core.ui.extensions.TextReference
import com.tangem.core.ui.extensions.stringReference
import com.tangem.features.jointaccount.common.displayname.ui.state.JointAccountDisplayNameUM
import com.tangem.test.core.ProvideTestModels
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.params.ParameterizedTest

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class UpdateDisplayNameInitialStateTransformerTest {

    @ParameterizedTest
    @ProvideTestModels
    fun transform(model: TransformModel) {
        // Arrange
        val transformer = createTransformer(initialName = model.initialName)

        // Act
        val actual = transformer.transform(prevState = emptyState)

        // Assert
        val expected = JointAccountDisplayNameUM(
            name = model.initialName,
            buttonText = buttonText,
            buttonIconRes = null,
            isButtonEnabled = model.expectedButtonEnabled,
            isButtonLoading = false,
            onNameChange = onNameChange,
            onContinueClick = onContinueClick,
            onBackClick = onBackClick,
        )
        assertThat(actual).isEqualTo(expected)
    }

    private fun provideTestModels() = listOf(
        // no name was entered before: the button text is set but the button stays locked
        TransformModel(initialName = "", expectedButtonEnabled = false),
        // a valid name restored from the draft unlocks the button right away
        TransformModel(initialName = "Ivan Zolo", expectedButtonEnabled = true),
        // letters of any script and digits are valid
        TransformModel(initialName = "Иван 2й", expectedButtonEnabled = true),
        // emoji are valid characters for a display name
        TransformModel(initialName = "Ivan 🚀", expectedButtonEnabled = true),
        // special characters are valid too
        TransformModel(initialName = "Anna-Maria O'Neil!", expectedButtonEnabled = true),
        // exactly 25 characters is the maximum allowed
        TransformModel(initialName = "a".repeat(n = 25), expectedButtonEnabled = true),
        // an over-long name does not unlock the button
        TransformModel(initialName = "a".repeat(n = 26), expectedButtonEnabled = false),
        // spaces are allowed characters, but a blank-only name is not a name
        TransformModel(initialName = "   ", expectedButtonEnabled = false),
    )

    internal data class TransformModel(
        val initialName: String,
        val expectedButtonEnabled: Boolean,
    )

    @Test
    fun `GIVEN state with icon and loader WHEN update initial state THEN both are kept`() {
        // Arrange
        val prevState = emptyState.copy(
            name = "Stale",
            buttonIconRes = R.drawable.ic_tangem_24,
            isButtonLoading = true,
        )
        val transformer = createTransformer(initialName = "Ivan Zolo")

        // Act
        val actual = transformer.transform(prevState)

        // Assert
        val expected = prevState.copy(
            name = "Ivan Zolo",
            buttonText = buttonText,
            isButtonEnabled = true,
            onNameChange = onNameChange,
            onContinueClick = onContinueClick,
            onBackClick = onBackClick,
        )
        assertThat(actual).isEqualTo(expected)
    }

    private fun createTransformer(initialName: String) = UpdateDisplayNameInitialStateTransformer(
        buttonText = buttonText,
        initialName = initialName,
        onNameChange = onNameChange,
        onContinueClick = onContinueClick,
        onBackClick = onBackClick,
    )

    private companion object {

        /** The state the controller starts with, before the model wires the callbacks */
        val emptyState = JointAccountDisplayNameUM(
            name = "",
            buttonText = TextReference.EMPTY,
            buttonIconRes = null,
            isButtonEnabled = false,
            isButtonLoading = false,
            onNameChange = {},
            onContinueClick = {},
            onBackClick = {},
        )

        // Shared instances so the expected state can pin the exact callbacks passed to the transformer
        val buttonText = stringReference(value = "Create account")
        val onNameChange: (String) -> Unit = {}
        val onContinueClick: () -> Unit = {}
        val onBackClick: () -> Unit = {}
    }
}