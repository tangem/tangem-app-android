package com.tangem.feature.wallet.child.wallet.model

import com.google.common.truth.Truth.assertThat
import com.tangem.domain.jointaccount.model.JointAccount
import com.tangem.domain.models.StatusSource
import com.tangem.domain.models.account.Account
import com.tangem.feature.wallet.presentation.wallet.analytics.WalletScreenAnalyticsEvent.MainScreen.ScreenOpened.JointAccountsCounts
import com.tangem.test.mock.MockAccounts
import org.junit.jupiter.api.Test

internal class JointAccountsCountsTest {

    private val accountList = MockAccounts.createAccountList(activeAccounts = 1, jointAccounts = 4)
    private val listedIds = accountList.accounts.filterIsInstance<Account.Joint>().map { it.accountId.value }

    @Test
    fun `GIVEN listed joint accounts in every status WHEN counted THEN pending and confirming count as pending`() {
        // Arrange
        val jointAccounts = listOf(
            jointAccount(id = listedIds[0], status = JointAccount.Status.ACTIVE),
            jointAccount(id = listedIds[1], status = JointAccount.Status.PENDING),
            jointAccount(id = listedIds[2], status = JointAccount.Status.CONFIRMING),
            jointAccount(id = listedIds[3], status = JointAccount.Status.CANCELLED),
        )

        // Act
        val actual = jointAccountsCounts(accountList = accountList, jointAccounts = jointAccounts)

        // Assert
        assertThat(actual).isEqualTo(JointAccountsCounts(active = 1, pending = 2))
    }

    @Test
    fun `GIVEN a joint account missing from the account list WHEN counted THEN it is left out as archived`() {
        // Arrange
        val jointAccounts = listOf(
            jointAccount(id = listedIds[0].uppercase(), status = JointAccount.Status.ACTIVE),
            jointAccount(id = "ab".repeat(n = 32), status = JointAccount.Status.ACTIVE),
        )

        // Act
        val actual = jointAccountsCounts(accountList = accountList, jointAccounts = jointAccounts)

        // Assert
        assertThat(actual).isEqualTo(JointAccountsCounts(active = 1, pending = 0))
    }

    private fun jointAccount(id: String, status: JointAccount.Status) = JointAccount(
        cryptoAccountId = id,
        membersCount = 3,
        threshold = 2,
        safeAddress = null,
        status = status,
        members = emptyList(),
        source = StatusSource.ACTUAL,
    )
}