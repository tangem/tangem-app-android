package com.tangem.features.jointaccount.creation.config.state.transformers

import com.google.common.truth.Truth.assertThat
import com.tangem.common.ui.account.AccountIconUM
import com.tangem.core.ui.extensions.stringReference
import com.tangem.domain.models.account.CryptoPortfolioIcon
import com.tangem.features.jointaccount.creation.config.ui.state.JointAccountConfigUM
import com.tangem.test.core.ProvideTestModels
import kotlinx.collections.immutable.toImmutableList
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.params.ParameterizedTest

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class SelectColorTransformerTest {

    @ParameterizedTest
    @ProvideTestModels
    fun transform(model: TransformModel) {
        // Arrange
        val prevState = createState(icon = SELECTED_ICON, color = model.prevColor)

        // Act
        val actual = SelectColorTransformer(color = model.color).transform(prevState)

        // Assert
        // The chosen icon is not affected by picking a color
        val expected = createState(icon = SELECTED_ICON, color = model.color)
        assertThat(actual).isEqualTo(expected)
    }

    private fun provideTestModels() = listOf(
        // picking another color replaces the current one
        TransformModel(
            prevColor = CryptoPortfolioIcon.Color.Azure,
            color = CryptoPortfolioIcon.Color.MexicanPink,
        ),
        // any color of the palette can be picked, including the last one
        TransformModel(
            prevColor = CryptoPortfolioIcon.Color.Azure,
            color = CryptoPortfolioIcon.Color.entries.last(),
        ),
        // re-picking the already selected color changes nothing
        TransformModel(
            prevColor = CryptoPortfolioIcon.Color.Pelati,
            color = CryptoPortfolioIcon.Color.Pelati,
        ),
        // colors can be switched away from a non-default one too
        TransformModel(
            prevColor = CryptoPortfolioIcon.Color.UFOGreen,
            color = CryptoPortfolioIcon.Color.CaribbeanBlue,
        ),
    )

    internal data class TransformModel(
        val prevColor: CryptoPortfolioIcon.Color,
        val color: CryptoPortfolioIcon.Color,
    )

    private fun createState(icon: CryptoPortfolioIcon.Icon, color: CryptoPortfolioIcon.Color): JointAccountConfigUM {
        return JointAccountConfigUM(
            name = "Family savings",
            namePlaceholder = namePlaceholder,
            icon = AccountIconUM.CryptoPortfolio(value = icon, color = color),
            colors = CryptoPortfolioIcon.Color.entries.toImmutableList(),
            icons = CryptoPortfolioIcon.Icon.entries.toImmutableList(),
            wallet = wallet,
            isContinueEnabled = true,
            onNameChange = onNameChange,
            onColorClick = onColorClick,
            onIconClick = onIconClick,
            onContinueClick = onContinueClick,
            onBackClick = onBackClick,
        )
    }

    private companion object {
        val SELECTED_ICON = CryptoPortfolioIcon.Icon.Home

        // Shared instances so states built before and after a transform compare equal
        val namePlaceholder = stringReference(value = "Joint account")
        val wallet = JointAccountConfigUM.WalletUM(name = "My wallet", onClick = {})
        val onNameChange: (String) -> Unit = {}
        val onColorClick: (CryptoPortfolioIcon.Color) -> Unit = {}
        val onIconClick: (CryptoPortfolioIcon.Icon) -> Unit = {}
        val onContinueClick: () -> Unit = {}
        val onBackClick: () -> Unit = {}
    }
}