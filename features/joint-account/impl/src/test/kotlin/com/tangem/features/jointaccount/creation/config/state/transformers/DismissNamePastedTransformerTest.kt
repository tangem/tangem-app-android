package com.tangem.features.jointaccount.creation.config.state.transformers

import com.google.common.truth.Truth.assertThat
import com.tangem.common.ui.account.AccountIconUM
import com.tangem.core.ui.extensions.stringReference
import com.tangem.domain.models.account.CryptoPortfolioIcon
import com.tangem.features.jointaccount.creation.config.ui.state.JointAccountConfigUM
import kotlinx.collections.immutable.persistentListOf
import org.junit.jupiter.api.Test

internal class DismissNamePastedTransformerTest {

    @Test
    fun `GIVEN cursor-to-end indication pending WHEN dismiss THEN only isValuePasted is cleared`() {
        // Arrange
        val prevState = createState(isValuePasted = true)

        // Act
        val actual = DismissNamePastedTransformer.transform(prevState)

        // Assert
        val expected = prevState.copy(isValuePasted = false)
        assertThat(actual).isEqualTo(expected)
    }

    private fun createState(isValuePasted: Boolean): JointAccountConfigUM {
        return JointAccountConfigUM(
            name = "Family savings",
            namePlaceholder = stringReference(value = "Joint account"),
            icon = AccountIconUM.CryptoPortfolio(
                value = CryptoPortfolioIcon.Icon.Family,
                color = CryptoPortfolioIcon.Color.Azure,
            ),
            colors = persistentListOf(),
            icons = persistentListOf(),
            wallet = null,
            isContinueEnabled = true,
            isValuePasted = isValuePasted,
            onValuePastedTriggerDismiss = {},
            onNameChange = { _, _ -> },
            onColorClick = {},
            onIconClick = {},
            onContinueClick = {},
            onBackClick = {},
        )
    }
}