package com.tangem.features.jointaccount.creation.config.state.transformers

import com.google.common.truth.Truth.assertThat
import com.tangem.common.ui.account.AccountIconUM
import com.tangem.core.ui.extensions.stringReference
import com.tangem.domain.models.account.CryptoPortfolioIcon
import com.tangem.features.jointaccount.creation.config.ui.state.JointAccountConfigUM
import com.tangem.test.core.ProvideTestModels
import kotlinx.collections.immutable.persistentListOf
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.params.ParameterizedTest

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class UpdateAccountNameTransformerTest {

    private val prevState = createState(name = "Old", isContinueEnabled = true)

    @ParameterizedTest
    @ProvideTestModels
    fun transform(model: TransformModel) {
        // Act
        val actual = UpdateAccountNameTransformer(name = model.input).transform(prevState)

        // Assert
        val expected = prevState.copy(name = model.expectedName, isContinueEnabled = model.expectedContinueEnabled)
        assertThat(actual).isEqualTo(expected)
    }

    private fun provideTestModels() = listOf(
        // typed input within the limit is applied as is
        TransformModel(input = "Family", expectedName = "Family", expectedContinueEnabled = true),
        // exactly 20 characters is the maximum allowed
        TransformModel(input = "a".repeat(n = 20), expectedName = "a".repeat(n = 20), expectedContinueEnabled = true),
        // the 21st character is not entered: the whole change is rejected, the previous value stays
        TransformModel(input = "a".repeat(n = 21), expectedName = "Old", expectedContinueEnabled = true),
        // an over-long paste is rejected entirely, not truncated
        TransformModel(input = "a".repeat(n = 40), expectedName = "Old", expectedContinueEnabled = true),
        // values are not normalized: surrounding spaces are kept verbatim
        TransformModel(input = " Alice ", expectedName = " Alice ", expectedContinueEnabled = true),
        // special characters and emoji are allowed for the account name
        TransformModel(input = "Fam!ly #1 🚀", expectedName = "Fam!ly #1 🚀", expectedContinueEnabled = true),
        // the name is required: clearing the field disables continue
        TransformModel(input = "", expectedName = "", expectedContinueEnabled = false),
        // blank-only input is not a valid name either
        TransformModel(input = "   ", expectedName = "   ", expectedContinueEnabled = false),
    )

    internal data class TransformModel(
        val input: String,
        val expectedName: String,
        val expectedContinueEnabled: Boolean,
    )

    private fun createState(name: String, isContinueEnabled: Boolean): JointAccountConfigUM {
        return JointAccountConfigUM(
            name = name,
            namePlaceholder = stringReference(value = "Joint account"),
            icon = AccountIconUM.CryptoPortfolio(
                value = CryptoPortfolioIcon.Icon.Family,
                color = CryptoPortfolioIcon.Color.Azure,
            ),
            colors = persistentListOf(),
            icons = persistentListOf(),
            wallet = null,
            chooseWallet = null,
            isContinueEnabled = isContinueEnabled,
            onNameChange = {},
            onColorClick = {},
            onIconClick = {},
            onContinueClick = {},
            onBackClick = {},
        )
    }
}