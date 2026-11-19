package com.tangem.features.jointaccount.creation.config.state.transformers

import com.google.common.truth.Truth.assertThat
import com.tangem.common.test.domain.wallet.MockUserWalletFactory
import com.tangem.common.ui.account.AccountIconUM
import com.tangem.core.ui.ds.image.DeviceIconUM
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
            selectedWalletIcon = model.selectedWalletIcon,
            onWalletRowClick = onWalletRowClick,
        )

        // Act
        val actual = transformer.transform(prevState = createState(wallet = model.prevWallet))

        // Assert
        assertThat(actual).isEqualTo(createState(wallet = model.expectedWallet))
    }

    private fun provideTestModels() = listOf(
        // a single wallet leaves nothing to choose, so the row is not shown
        TransformModel(
            wallets = listOf(firstWallet),
            selectedWallet = firstWallet,
            selectedWalletIcon = cardIcon,
            prevWallet = null,
            expectedWallet = null,
        ),
        // several wallets show the row with the selected wallet name
        TransformModel(
            wallets = listOf(firstWallet, secondWallet),
            selectedWallet = secondWallet,
            selectedWalletIcon = cardIcon,
            prevWallet = null,
            expectedWallet = walletRow(secondWallet),
        ),
        // selecting another wallet updates the name shown in the row
        TransformModel(
            wallets = listOf(firstWallet, secondWallet, thirdWallet),
            selectedWallet = thirdWallet,
            selectedWalletIcon = cardIcon,
            prevWallet = walletRow(firstWallet),
            expectedWallet = walletRow(thirdWallet),
        ),
        // the row shows the icon resolved for the selected wallet as is
        TransformModel(
            wallets = listOf(firstWallet, secondWallet),
            selectedWallet = secondWallet,
            selectedWalletIcon = DeviceIconUM.Mobile,
            prevWallet = null,
            expectedWallet = walletRow(secondWallet, deviceIcon = DeviceIconUM.Mobile),
        ),
        // no selected wallet yet: the row cannot be rendered
        TransformModel(
            wallets = listOf(firstWallet, secondWallet),
            selectedWallet = null,
            selectedWalletIcon = null,
            prevWallet = null,
            expectedWallet = null,
        ),
        // the wallets list shrank to one: the previously shown row is hidden again
        TransformModel(
            wallets = listOf(firstWallet),
            selectedWallet = firstWallet,
            selectedWalletIcon = cardIcon,
            prevWallet = walletRow(secondWallet),
            expectedWallet = null,
        ),
        // no wallets at all
        TransformModel(
            wallets = emptyList(),
            selectedWallet = null,
            selectedWalletIcon = null,
            prevWallet = null,
            expectedWallet = null,
        ),
    )

    internal data class TransformModel(
        val wallets: List<UserWallet>,
        val selectedWallet: UserWallet?,
        val selectedWalletIcon: DeviceIconUM?,
        val prevWallet: JointAccountConfigUM.WalletUM?,
        val expectedWallet: JointAccountConfigUM.WalletUM?,
    )

    private fun walletRow(wallet: UserWallet, deviceIcon: DeviceIconUM = cardIcon) = JointAccountConfigUM.WalletUM(
        name = wallet.name,
        deviceIcon = deviceIcon,
        onClick = onWalletRowClick,
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
            isValuePasted = false,
            onValuePastedTriggerDismiss = {},
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
        val cardIcon = DeviceIconUM.Stub(cardsCount = 1)

        // Shared instances so states built before and after a transform compare equal
        val namePlaceholder = stringReference(value = "Joint account")
        val onWalletRowClick: () -> Unit = {}
        val onNameChange: (String, Boolean) -> Unit = { _, _ -> }
        val onColorClick: (CryptoPortfolioIcon.Color) -> Unit = {}
        val onIconClick: (CryptoPortfolioIcon.Icon) -> Unit = {}
        val onContinueClick: () -> Unit = {}
        val onBackClick: () -> Unit = {}
    }
}