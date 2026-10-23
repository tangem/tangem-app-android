package com.tangem.features.jointaccount.common.displayname.state.transformers

import com.google.common.truth.Truth.assertThat
import com.tangem.core.ui.R
import com.tangem.core.ui.ds.image.TangemIconUM
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
        val prevState = createState(buttonIcon = model.prevIcon)

        // Act
        val actual = UpdateButtonIconTransformer(icon = model.icon).transform(prevState)

        // Assert
        assertThat(actual).isEqualTo(createState(buttonIcon = model.expectedIcon))
    }

    private fun provideTestModels() = listOf(
        // a cold wallet contributes its NFC icon to the button
        TransformModel(prevIcon = null, icon = TANGEM_ICON, expectedIcon = TANGEM_ICON),
        // a hot wallet has no wallet-interaction icon, so the previous one is cleared
        TransformModel(prevIcon = TANGEM_ICON, icon = null, expectedIcon = null),
        // re-emitting the same icon leaves the state as is
        TransformModel(prevIcon = TANGEM_ICON, icon = TANGEM_ICON, expectedIcon = TANGEM_ICON),
        // an already empty icon stays empty
        TransformModel(prevIcon = null, icon = null, expectedIcon = null),
        // another wallet kind replaces the icon
        TransformModel(prevIcon = TANGEM_ICON, icon = WALLET_ICON, expectedIcon = WALLET_ICON),
    )

    internal data class TransformModel(
        val prevIcon: TangemIconUM?,
        val icon: TangemIconUM?,
        val expectedIcon: TangemIconUM?,
    )

    /** The rest of the state is filled with non-default values to pin that the transformer touches only the icon */
    private fun createState(buttonIcon: TangemIconUM?): JointAccountDisplayNameUM {
        return JointAccountDisplayNameUM(
            name = "Ivan Zolo",
            buttonText = buttonText,
            buttonIcon = buttonIcon,
            isButtonEnabled = true,
            isButtonLoading = true,
            onNameChange = onNameChange,
            onContinueClick = onContinueClick,
            onBackClick = onBackClick,
        )
    }

    private companion object {
        val TANGEM_ICON = TangemIconUM.Icon(iconRes = R.drawable.ic_tangem_24)
        val WALLET_ICON = TangemIconUM.Icon(iconRes = R.drawable.ic_wallet_24)

        // Shared instances so states built before and after a transform compare equal
        val buttonText = stringReference(value = "Create account")
        val onNameChange: (String) -> Unit = {}
        val onContinueClick: () -> Unit = {}
        val onBackClick: () -> Unit = {}
    }
}