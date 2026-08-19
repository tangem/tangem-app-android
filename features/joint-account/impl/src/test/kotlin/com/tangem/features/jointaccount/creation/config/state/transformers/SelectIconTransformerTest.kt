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
internal class SelectIconTransformerTest {

    @ParameterizedTest
    @ProvideTestModels
    fun transform(model: TransformModel) {
        // Arrange
        val prevState = createState(icon = model.prevIcon, color = SELECTED_COLOR)

        // Act
        val actual = SelectIconTransformer(icon = model.icon).transform(prevState)

        // Assert
        // The chosen color is not affected by picking an icon
        val expected = createState(icon = model.icon, color = SELECTED_COLOR)
        assertThat(actual).isEqualTo(expected)
    }

    private fun provideTestModels() = listOf(
        // picking another icon replaces the current one
        TransformModel(
            prevIcon = CryptoPortfolioIcon.Icon.Family,
            icon = CryptoPortfolioIcon.Icon.Beach,
        ),
        // any icon of the grid can be picked, including the last one
        TransformModel(
            prevIcon = CryptoPortfolioIcon.Icon.Family,
            icon = CryptoPortfolioIcon.Icon.entries.last(),
        ),
        // re-picking the already selected icon changes nothing
        TransformModel(
            prevIcon = CryptoPortfolioIcon.Icon.Safe,
            icon = CryptoPortfolioIcon.Icon.Safe,
        ),
        // icons can be switched away from a non-default one too
        TransformModel(
            prevIcon = CryptoPortfolioIcon.Icon.Money,
            icon = CryptoPortfolioIcon.Icon.Letter,
        ),
    )

    internal data class TransformModel(
        val prevIcon: CryptoPortfolioIcon.Icon,
        val icon: CryptoPortfolioIcon.Icon,
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
        val SELECTED_COLOR = CryptoPortfolioIcon.Color.MexicanPink

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