package com.tangem.features.jointaccount.creation.config.state.transformers

import com.google.common.truth.Truth.assertThat
import com.tangem.common.test.domain.wallet.MockUserWalletFactory
import com.tangem.common.ui.account.AccountIconUM
import com.tangem.core.ui.extensions.stringReference
import com.tangem.domain.models.account.CryptoPortfolioIcon
import com.tangem.domain.models.wallet.UserWallet
import com.tangem.domain.models.wallet.UserWalletId
import com.tangem.features.jointaccount.creation.config.ui.state.JointAccountConfigUM
import com.tangem.test.core.ProvideTestModels
import kotlinx.collections.immutable.toImmutableList
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.params.ParameterizedTest

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class UpdateWalletsTransformerTest {

    @ParameterizedTest
    @ProvideTestModels
    fun transform(model: TransformModel) {
        // Arrange
        val transformer = UpdateWalletsTransformer(
            wallets = model.wallets,
            selectedWallet = model.selectedWallet,
            onWalletRowClick = onWalletRowClick,
        )

        // Act
        val actual = transformer.transform(prevState = createState(wallet = model.prevWallet))

        // Assert
        val expectedWallet = model.expectedWalletName?.let {
            JointAccountConfigUM.WalletUM(name = it, onClick = onWalletRowClick)
        }
        assertThat(actual).isEqualTo(createState(wallet = expectedWallet))
    }

    private fun provideTestModels() = listOf(
        // a single wallet leaves nothing to choose, so the row is not shown
        TransformModel(
            wallets = listOf(firstWallet),
            selectedWallet = firstWallet,
            prevWallet = null,
            expectedWalletName = null,
        ),
        // several wallets show the row with the selected wallet name
        TransformModel(
            wallets = listOf(firstWallet, secondWallet),
            selectedWallet = secondWallet,
            prevWallet = null,
            expectedWalletName = secondWallet.name,
        ),
        // selecting another wallet updates the name shown in the row
        TransformModel(
            wallets = listOf(firstWallet, secondWallet, thirdWallet),
            selectedWallet = thirdWallet,
            prevWallet = JointAccountConfigUM.WalletUM(name = firstWallet.name, onClick = onWalletRowClick),
            expectedWalletName = thirdWallet.name,
        ),
        // no selected wallet yet: the row cannot be rendered
        TransformModel(
            wallets = listOf(firstWallet, secondWallet),
            selectedWallet = null,
            prevWallet = null,
            expectedWalletName = null,
        ),
        // the wallets list shrank to one: the previously shown row is hidden again
        TransformModel(
            wallets = listOf(firstWallet),
            selectedWallet = firstWallet,
            prevWallet = JointAccountConfigUM.WalletUM(name = secondWallet.name, onClick = onWalletRowClick),
            expectedWalletName = null,
        ),
        // no wallets at all
        TransformModel(
            wallets = emptyList(),
            selectedWallet = null,
            prevWallet = null,
            expectedWalletName = null,
        ),
    )

    internal data class TransformModel(
        val wallets: List<UserWallet>,
        val selectedWallet: UserWallet?,
        val prevWallet: JointAccountConfigUM.WalletUM?,
        val expectedWalletName: String?,
    )

    private fun createState(wallet: JointAccountConfigUM.WalletUM?): JointAccountConfigUM {
        return JointAccountConfigUM(
            name = "Family savings",
            namePlaceholder = namePlaceholder,
            icon = AccountIconUM.CryptoPortfolio(
                value = CryptoPortfolioIcon.Icon.Family,
                color = CryptoPortfolioIcon.Color.Azure,
            ),
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

        val firstWallet = MockUserWalletFactory.create().copy(
            walletId = UserWalletId(stringValue = "011"),
            name = "My wallet",
        )
        val secondWallet = MockUserWalletFactory.create().copy(
            walletId = UserWalletId(stringValue = "012"),
            name = "My wallet 2",
        )
        val thirdWallet = MockUserWalletFactory.create().copy(
            walletId = UserWalletId(stringValue = "013"),
            name = "My wallet 3",
        )

        // Shared instances so states built before and after a transform compare equal
        val namePlaceholder = stringReference(value = "Joint account")
        val onWalletRowClick: () -> Unit = {}
        val onNameChange: (String) -> Unit = {}
        val onColorClick: (CryptoPortfolioIcon.Color) -> Unit = {}
        val onIconClick: (CryptoPortfolioIcon.Icon) -> Unit = {}
        val onContinueClick: () -> Unit = {}
        val onBackClick: () -> Unit = {}
    }
}