package com.tangem.features.jointaccount.creation.config.state.transformers

import com.google.common.truth.Truth.assertThat
import com.tangem.common.ui.account.AccountIconUM
import com.tangem.core.ui.extensions.stringReference
import com.tangem.domain.models.account.CryptoPortfolioIcon
import com.tangem.features.jointaccount.creation.config.ui.state.JointAccountConfigUM
import kotlinx.collections.immutable.toImmutableList
import org.junit.jupiter.api.Test

internal class UpdateConfigInitialStateTransformerTest {

    private val transformer = UpdateConfigInitialStateTransformer(
        onNameChange = onNameChange,
        onColorClick = onColorClick,
        onIconClick = onIconClick,
        onContinueClick = onContinueClick,
        onBackClick = onBackClick,
    )

    @Test
    fun `GIVEN empty state WHEN update initial state THEN only callbacks are wired`() {
        // Act
        val actual = transformer.transform(prevState = emptyState)

        // Assert
        val expected = JointAccountConfigUM(
            name = "",
            namePlaceholder = namePlaceholder,
            icon = AccountIconUM.CryptoPortfolio(
                value = CryptoPortfolioIcon.Icon.Family,
                color = CryptoPortfolioIcon.Color.Azure,
            ),
            colors = CryptoPortfolioIcon.Color.entries.toImmutableList(),
            icons = CryptoPortfolioIcon.Icon.entries.toImmutableList(),
            wallet = null,
            isContinueEnabled = false,
            onNameChange = onNameChange,
            onColorClick = onColorClick,
            onIconClick = onIconClick,
            onContinueClick = onContinueClick,
            onBackClick = onBackClick,
        )
        assertThat(actual).isEqualTo(expected)
    }

    @Test
    fun `GIVEN filled state WHEN update initial state THEN entered config survives`() {
        // Arrange
        val filledState = emptyState.copy(
            name = "Family savings",
            icon = AccountIconUM.CryptoPortfolio(
                value = CryptoPortfolioIcon.Icon.Home,
                color = CryptoPortfolioIcon.Color.MexicanPink,
            ),
            wallet = JointAccountConfigUM.WalletUM(name = "My wallet", onClick = onWalletClick),
            isContinueEnabled = true,
        )

        // Act
        val actual = transformer.transform(prevState = filledState)

        // Assert
        val expected = filledState.copy(
            onNameChange = onNameChange,
            onColorClick = onColorClick,
            onIconClick = onIconClick,
            onContinueClick = onContinueClick,
            onBackClick = onBackClick,
        )
        assertThat(actual).isEqualTo(expected)
    }

    private companion object {

        val namePlaceholder = stringReference(value = "Joint account")

        /** The state the controller starts with, before the model wires the callbacks */
        val emptyState = JointAccountConfigUM(
            name = "",
            namePlaceholder = namePlaceholder,
            icon = AccountIconUM.CryptoPortfolio(
                value = CryptoPortfolioIcon.Icon.Family,
                color = CryptoPortfolioIcon.Color.Azure,
            ),
            colors = CryptoPortfolioIcon.Color.entries.toImmutableList(),
            icons = CryptoPortfolioIcon.Icon.entries.toImmutableList(),
            wallet = null,
            isContinueEnabled = false,
            onNameChange = {},
            onColorClick = {},
            onIconClick = {},
            onContinueClick = {},
            onBackClick = {},
        )

        // Shared instances so the expected state can pin the exact callbacks passed to the transformer
        val onNameChange: (String) -> Unit = {}
        val onColorClick: (CryptoPortfolioIcon.Color) -> Unit = {}
        val onIconClick: (CryptoPortfolioIcon.Icon) -> Unit = {}
        val onContinueClick: () -> Unit = {}
        val onBackClick: () -> Unit = {}
        val onWalletClick: () -> Unit = {}
    }
}