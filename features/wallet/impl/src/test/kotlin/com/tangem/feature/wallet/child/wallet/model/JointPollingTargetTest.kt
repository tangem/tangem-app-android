package com.tangem.feature.wallet.child.wallet.model

import com.google.common.truth.Truth.assertThat
import com.tangem.domain.account.models.AccountStatusList
import com.tangem.domain.models.account.AccountStatus
import com.tangem.domain.models.wallet.UserWalletId
import com.tangem.test.core.ProvideTestModels
import io.mockk.every
import io.mockk.mockk
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.params.ParameterizedTest

/**
 * [jointPollingTarget] is the gate `WalletModel.subscribeJointAccountPolling` polls through — pulled out as a
 * pure top-level function so it's testable without standing up a [WalletModel] instance (its constructor is too
 * large for that to be worthwhile for this one gate).
 */
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class JointPollingTargetTest {

    @ParameterizedTest
    @ProvideTestModels
    fun `GIVEN screen state WHEN resolving the polling target THEN only a foregrounded, selected, joint wallet qualifies`(
        model: TestModel,
    ) {
        // Act
        val actual = jointPollingTarget(
            isBackground = model.isBackground,
            selectedWalletId = model.selectedWalletId,
            accountStatusLists = model.accountStatusLists,
        )

        // Assert
        assertThat(actual).isEqualTo(model.expected)
    }

    private fun provideTestModels() = listOf(
        TestModel(
            name = "backgrounded screen never polls, even with a selected joint wallet",
            isBackground = true,
            selectedWalletId = WALLET_A,
            accountStatusLists = listOf(accountStatusList(WALLET_A, hasJoint = true)),
            expected = null,
        ),
        TestModel(
            name = "no wallet selected yet",
            isBackground = false,
            selectedWalletId = null,
            accountStatusLists = listOf(accountStatusList(WALLET_A, hasJoint = true)),
            expected = null,
        ),
        TestModel(
            name = "selected wallet has no joint account",
            isBackground = false,
            selectedWalletId = WALLET_A,
            accountStatusLists = listOf(accountStatusList(WALLET_A, hasJoint = false)),
            expected = null,
        ),
        TestModel(
            name = "a different wallet has a joint account, but it isn't the selected one",
            isBackground = false,
            selectedWalletId = WALLET_A,
            accountStatusLists = listOf(
                accountStatusList(WALLET_A, hasJoint = false),
                accountStatusList(WALLET_B, hasJoint = true),
            ),
            expected = null,
        ),
        TestModel(
            name = "foregrounded, selected wallet has a joint account",
            isBackground = false,
            selectedWalletId = WALLET_A,
            accountStatusLists = listOf(accountStatusList(WALLET_A, hasJoint = true)),
            expected = WALLET_A,
        ),
    )

    data class TestModel(
        val name: String,
        val isBackground: Boolean,
        val selectedWalletId: UserWalletId?,
        val accountStatusLists: List<AccountStatusList>,
        val expected: UserWalletId?,
    ) {
        override fun toString() = name
    }

    private fun accountStatusList(walletId: UserWalletId, hasJoint: Boolean): AccountStatusList {
        val status = if (hasJoint) mockk<AccountStatus.Joint>() else mockk<AccountStatus.Personal>()
        return mockk {
            every { userWalletId } returns walletId
            every { accountStatuses } returns listOf(status)
        }
    }

    private companion object {
        val WALLET_A = UserWalletId("011")
        val WALLET_B = UserWalletId("012")
    }
}