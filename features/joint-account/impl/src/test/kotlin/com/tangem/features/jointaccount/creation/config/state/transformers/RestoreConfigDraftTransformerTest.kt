package com.tangem.features.jointaccount.creation.config.state.transformers

import com.google.common.truth.Truth.assertThat
import com.tangem.common.ui.account.AccountIconUM
import com.tangem.core.ui.extensions.stringReference
import com.tangem.domain.models.account.CryptoPortfolioIcon
import com.tangem.domain.models.wallet.UserWalletId
import com.tangem.features.jointaccount.creation.config.ui.state.JointAccountConfigUM
import com.tangem.features.jointaccount.creation.model.JointAccountCreationDraft
import com.tangem.test.core.ProvideTestModels
import kotlinx.collections.immutable.toImmutableList
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.params.ParameterizedTest

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class RestoreConfigDraftTransformerTest {

    private val prevState = createState(
        name = "Typed",
        icon = CryptoPortfolioIcon.Icon.Star,
        color = CryptoPortfolioIcon.Color.Pelati,
        isContinueEnabled = false,
    )

    @ParameterizedTest
    @ProvideTestModels
    fun transform(model: TransformModel) {
        // Act
        val actual = RestoreConfigDraftTransformer(draft = model.draft).transform(prevState)

        // Assert
        val expected = createState(
            name = model.expectedName,
            icon = model.expectedIcon,
            color = model.expectedColor,
            // The draft is only saved for a valid config, so restoring it always unlocks Continue
            isContinueEnabled = true,
        )
        assertThat(actual).isEqualTo(expected)
    }

    private fun provideTestModels() = listOf(
        // every draft-backed field overrides what the state held
        TransformModel(
            draft = createDraft(
                name = "Family savings",
                icon = CryptoPortfolioIcon.Icon.Home,
                color = CryptoPortfolioIcon.Color.MexicanPink,
            ),
            expectedName = "Family savings",
            expectedIcon = CryptoPortfolioIcon.Icon.Home,
            expectedColor = CryptoPortfolioIcon.Color.MexicanPink,
        ),
        // a draft equal to the current icon and color changes the name only
        TransformModel(
            draft = createDraft(
                name = "Vacation",
                icon = CryptoPortfolioIcon.Icon.Star,
                color = CryptoPortfolioIcon.Color.Pelati,
            ),
            expectedName = "Vacation",
            expectedIcon = CryptoPortfolioIcon.Icon.Star,
            expectedColor = CryptoPortfolioIcon.Color.Pelati,
        ),
        // the saved name is restored verbatim, spaces and emoji included
        TransformModel(
            draft = createDraft(
                name = " Fam!ly 🚀 ",
                icon = CryptoPortfolioIcon.Icon.Letter,
                color = CryptoPortfolioIcon.Color.VitalGreen,
            ),
            expectedName = " Fam!ly 🚀 ",
            expectedIcon = CryptoPortfolioIcon.Icon.Letter,
            expectedColor = CryptoPortfolioIcon.Color.VitalGreen,
        ),
    )

    internal data class TransformModel(
        val draft: JointAccountCreationDraft.Config,
        val expectedName: String,
        val expectedIcon: CryptoPortfolioIcon.Icon,
        val expectedColor: CryptoPortfolioIcon.Color,
    )

    private fun createDraft(
        name: String,
        icon: CryptoPortfolioIcon.Icon,
        color: CryptoPortfolioIcon.Color,
    ) = JointAccountCreationDraft.Config(
        name = name,
        icon = icon,
        color = color,
        walletId = UserWalletId(stringValue = "011"),
    )

    private fun createState(
        name: String,
        icon: CryptoPortfolioIcon.Icon,
        color: CryptoPortfolioIcon.Color,
        isContinueEnabled: Boolean,
    ): JointAccountConfigUM {
        return JointAccountConfigUM(
            name = name,
            namePlaceholder = namePlaceholder,
            icon = AccountIconUM.CryptoPortfolio(value = icon, color = color),
            colors = CryptoPortfolioIcon.Color.entries.toImmutableList(),
            icons = CryptoPortfolioIcon.Icon.entries.toImmutableList(),
            wallet = wallet,
            isContinueEnabled = isContinueEnabled,
            onNameChange = onNameChange,
            onColorClick = onColorClick,
            onIconClick = onIconClick,
            onContinueClick = onContinueClick,
            onBackClick = onBackClick,
        )
    }

    private companion object {
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