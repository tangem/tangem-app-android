package com.tangem.features.jointaccount.common.displayname.state.transformers

import com.google.common.truth.Truth.assertThat
import com.tangem.core.ui.R
import com.tangem.core.ui.extensions.stringReference
import com.tangem.features.jointaccount.common.displayname.ui.state.JointAccountDisplayNameUM
import com.tangem.test.core.ProvideTestModels
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.params.ParameterizedTest

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class UpdateButtonIconTransformerTest {

    @ParameterizedTest
    @ProvideTestModels
    fun transform(model: TransformModel) {
        // Arrange
        val prevState = createState(buttonIconRes = model.prevIconRes)

        // Act
        val actual = UpdateButtonIconTransformer(iconRes = model.iconRes).transform(prevState)

        // Assert
        assertThat(actual).isEqualTo(createState(buttonIconRes = model.expectedIconRes))
    }

    private fun provideTestModels() = listOf(
        // a cold wallet contributes its NFC icon to the button
        TransformModel(prevIconRes = null, iconRes = TANGEM_ICON, expectedIconRes = TANGEM_ICON),
        // a hot wallet has no wallet-interaction icon, so the previous one is cleared
        TransformModel(prevIconRes = TANGEM_ICON, iconRes = null, expectedIconRes = null),
        // re-emitting the same icon leaves the state as is
        TransformModel(prevIconRes = TANGEM_ICON, iconRes = TANGEM_ICON, expectedIconRes = TANGEM_ICON),
        // an already empty icon stays empty
        TransformModel(prevIconRes = null, iconRes = null, expectedIconRes = null),
        // another wallet kind replaces the icon
        TransformModel(prevIconRes = TANGEM_ICON, iconRes = WALLET_ICON, expectedIconRes = WALLET_ICON),
    )

    internal data class TransformModel(
        val prevIconRes: Int?,
        val iconRes: Int?,
        val expectedIconRes: Int?,
    )

    /** The rest of the state is filled with non-default values to pin that the transformer touches only the icon */
    private fun createState(buttonIconRes: Int?): JointAccountDisplayNameUM {
        return JointAccountDisplayNameUM(
            name = "Ivan Zolo",
            isError = false,
            buttonText = buttonText,
            buttonIconRes = buttonIconRes,
            isButtonEnabled = true,
            isButtonLoading = true,
            onNameChange = onNameChange,
            onContinueClick = onContinueClick,
            onBackClick = onBackClick,
        )
    }

    private companion object {
        val TANGEM_ICON = R.drawable.ic_tangem_24
        val WALLET_ICON = R.drawable.ic_wallet_24

        // Shared instances so states built before and after a transform compare equal
        val buttonText = stringReference(value = "Create account")
        val onNameChange: (String) -> Unit = {}
        val onContinueClick: () -> Unit = {}
        val onBackClick: () -> Unit = {}
    }
}