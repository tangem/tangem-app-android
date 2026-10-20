package com.tangem.feature.wallet.presentation.wallet.domain

import com.google.common.truth.Truth.assertThat
import com.tangem.domain.account.models.AccountStatusList
import com.tangem.domain.account.status.producer.SingleAccountStatusListProducer
import com.tangem.domain.account.status.supplier.SingleAccountStatusListSupplier
import com.tangem.domain.jointaccount.model.JointPendingOperation
import com.tangem.domain.jointaccount.usecase.GetPendingJointOperationsUseCase
import com.tangem.domain.models.account.Account
import com.tangem.domain.models.account.AccountStatus
import com.tangem.domain.models.account.JointAccountStatusValue
import com.tangem.domain.models.wallet.UserWallet
import com.tangem.domain.models.wallet.UserWalletId
import io.mockk.clearMocks
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

internal class GetPendingJointOperationsFlowFactoryTest {

    private val singleAccountStatusListSupplier: SingleAccountStatusListSupplier = mockk()
    private val getPendingJointOperationsUseCase: GetPendingJointOperationsUseCase = mockk()
    private val userWallet: UserWallet = mockk()

    private val factory = GetPendingJointOperationsFlowFactory(
        singleAccountStatusListSupplier = singleAccountStatusListSupplier,
        getPendingJointOperationsUseCase = getPendingJointOperationsUseCase,
    )

    @BeforeEach
    fun setup() {
        clearMocks(singleAccountStatusListSupplier, getPendingJointOperationsUseCase, userWallet)
        every { userWallet.walletId } returns WALLET_ID
    }

    @Test
    fun `GIVEN no joint accounts WHEN create THEN emits no operations without asking for them`() = runTest {
        // Arrange
        val personal = mockk<AccountStatus.Personal>()
        every { singleAccountStatusListSupplier(params) } returns flowOf(accountStatusList(personal))

        // Act
        val actual = factory.create(userWallet).first()

        // Assert
        assertThat(actual).isEmpty()
        verify(exactly = 0) { getPendingJointOperationsUseCase(any(), any()) }
    }

    @Test
    fun `GIVEN joint accounts WHEN create THEN emits their pending operations`() = runTest {
        // Arrange
        val account = mockk<Account.Joint>()
        val jointStatus = jointStatus(account = account, value = mockk<JointAccountStatusValue.Active>())
        val operations = listOf(mockk<JointPendingOperation>())
        every { singleAccountStatusListSupplier(params) } returns flowOf(accountStatusList(jointStatus))
        every { getPendingJointOperationsUseCase(WALLET_ID, listOf(account)) } returns flowOf(operations)

        // Act
        val actual = factory.create(userWallet).first()

        // Assert
        assertThat(actual).isEqualTo(operations)
    }

    @Test
    fun `GIVEN joint account not active WHEN create THEN emits no operations without asking for them`() = runTest {
        // Arrange
        val jointStatus = jointStatus(account = mockk(), value = mockk<JointAccountStatusValue.Error.Compromised>())
        every { singleAccountStatusListSupplier(params) } returns flowOf(accountStatusList(jointStatus))

        // Act
        val actual = factory.create(userWallet).first()

        // Assert
        assertThat(actual).isEmpty()
        verify(exactly = 0) { getPendingJointOperationsUseCase(any(), any()) }
    }

    private fun jointStatus(account: Account.Joint, value: JointAccountStatusValue): AccountStatus.Joint = mockk {
        every { this@mockk.account } returns account
        every { this@mockk.value } returns value
    }

    private fun accountStatusList(vararg statuses: AccountStatus): AccountStatusList = mockk {
        every { accountStatuses } returns statuses.toList()
    }

    private companion object {
        val WALLET_ID = UserWalletId("01")
        val params = SingleAccountStatusListProducer.Params(WALLET_ID)
    }
}