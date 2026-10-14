package com.tangem.features.jointaccount.compromised

import com.google.common.truth.Truth.assertThat
import com.tangem.core.decompose.ui.UiMessageSender
import com.tangem.domain.account.models.AccountStatusList
import com.tangem.domain.account.status.supplier.SingleAccountStatusListSupplier
import com.tangem.domain.core.utils.lceLoading
import com.tangem.domain.models.StatusSource
import com.tangem.domain.models.TotalFiatBalance
import com.tangem.domain.models.account.AccountStatus
import com.tangem.domain.models.account.JointAccountStatusValue
import com.tangem.domain.models.tokenlist.TokenList
import com.tangem.domain.models.wallet.UserWalletId
import com.tangem.test.mock.MockAccounts
import io.mockk.clearMocks
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

internal class DefaultJointAccountCompromisedAlertTest {

    private val uiMessageSender: UiMessageSender = mockk(relaxed = true)
    private val singleAccountStatusListSupplier: SingleAccountStatusListSupplier = mockk()

    private val alert = DefaultJointAccountCompromisedAlert(
        uiMessageSender = uiMessageSender,
        getWalletMetaInfoUseCase = mockk(),
        sendFeedbackEmailUseCase = mockk(),
        appScope = mockk(),
        singleAccountStatusListSupplier = singleAccountStatusListSupplier,
    )

    private val userWalletId = UserWalletId("011")
    private val jointAccount = MockAccounts.createJointAccount(userWalletId = userWalletId)

    @BeforeEach
    fun resetMocks() {
        clearMocks(uiMessageSender, singleAccountStatusListSupplier)
    }

    @Test
    fun `GIVEN compromised joint account WHEN showIfCompromised THEN the alert is shown`() = runTest {
        // Arrange
        stubStatus(
            JointAccountStatusValue.Error.Compromised(
                totalMembers = 2,
                tokensCount = 0,
                fiatBalance = TotalFiatBalance.Loading,
            ),
        )

        // Act
        val actual = alert.showIfCompromised(userWalletId = userWalletId, accountId = jointAccount.accountId.value)

        // Assert
        assertThat(actual).isTrue()
        verify(exactly = 1) { uiMessageSender.send(any()) }
    }

    @Test
    fun `GIVEN active joint account WHEN showIfCompromised THEN nothing is shown`() = runTest {
        // Arrange
        stubStatus(JointAccountStatusValue.Active(StatusSource.ACTUAL, totalMembers = 2, safeAddress = "0x1"))

        // Act
        val actual = alert.showIfCompromised(userWalletId = userWalletId, accountId = jointAccount.accountId.value)

        // Assert
        assertThat(actual).isFalse()
        verify(exactly = 0) { uiMessageSender.send(any()) }
    }

    private fun stubStatus(value: JointAccountStatusValue) {
        val status = AccountStatus.Joint(
            account = jointAccount,
            value = value,
            tokenList = TokenList.Empty,
            priceChangeLce = lceLoading(),
        )
        val list = mockk<AccountStatusList> { every { accountStatuses } returns listOf(status) }
        coEvery { singleAccountStatusListSupplier.getSyncOrNull(userWalletId) } returns list
    }
}