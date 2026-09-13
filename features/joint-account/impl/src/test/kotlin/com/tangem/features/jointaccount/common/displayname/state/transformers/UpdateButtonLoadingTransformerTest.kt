package com.tangem.features.jointaccount.common.displayname.state.transformers

import com.google.common.truth.Truth.assertThat
import com.tangem.core.ui.R
import com.tangem.core.ui.extensions.stringReference
import com.tangem.features.jointaccount.common.displayname.ui.state.JointAccountDisplayNameUM
import com.tangem.test.core.ProvideTestModels
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.params.ParameterizedTest

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class UpdateButtonLoadingTransformerTest {

    @ParameterizedTest
    @ProvideTestModels
    fun transform(model: TransformModel) {
        // Arrange
        val prevState = createState(isButtonLoading = model.prevLoading)

        // Act
        val actual = UpdateButtonLoadingTransformer(isLoading = model.isLoading).transform(prevState)

        // Assert
        assertThat(actual).isEqualTo(createState(isButtonLoading = model.isLoading))
    }

    private fun provideTestModels() = listOf(
        // starting the signature session shows the loader
        TransformModel(prevLoading = false, isLoading = true),
        // finishing the session returns the button to its normal content
        TransformModel(prevLoading = true, isLoading = false),
        // toggling to the same value changes nothing
        TransformModel(prevLoading = true, isLoading = true),
        TransformModel(prevLoading = false, isLoading = false),
    )

    internal data class TransformModel(
        val prevLoading: Boolean,
        val isLoading: Boolean,
    )

    /**
     * The button stays enabled and the name stays filled: the loading flag is the only thing the transformer owns,
     * it must not re-derive the validation state
     */
    private fun createState(isButtonLoading: Boolean): JointAccountDisplayNameUM {
        return JointAccountDisplayNameUM(
            name = "Ivan Zolo",
            isError = false,
            buttonText = buttonText,
            buttonIconRes = R.drawable.ic_tangem_24,
            isButtonEnabled = true,
            isButtonLoading = isButtonLoading,
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