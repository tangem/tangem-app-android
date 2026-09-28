package com.tangem.domain.moneyaccount.usecase

import arrow.core.left
import arrow.core.right
import com.google.common.truth.Truth.assertThat
import com.tangem.domain.models.account.Account
import com.tangem.domain.models.account.AccountStatus
import com.tangem.domain.models.account.MoneyAccountStatusValue
import com.tangem.domain.models.wallet.UserWalletId
import com.tangem.domain.moneyaccount.flow.MoneyAccountStatusFetcher
import com.tangem.domain.moneyaccount.flow.MoneyAccountStatusSupplier
import io.mockk.clearMocks
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

internal class GetMoneyAccountStatusUseCaseTest {

    private val moneyAccountStatusFetcher: MoneyAccountStatusFetcher = mockk()
    private val moneyAccountStatusSupplier: MoneyAccountStatusSupplier = mockk()
    private val userWalletId: UserWalletId = mockk()

    private val useCase = GetMoneyAccountStatusUseCase(
        moneyAccountStatusFetcher = moneyAccountStatusFetcher,
        moneyAccountStatusSupplier = moneyAccountStatusSupplier,
    )

    @BeforeEach
    fun setup() {
        clearMocks(moneyAccountStatusFetcher, moneyAccountStatusSupplier)
    }

    @Test
    fun `GIVEN fetch succeeds WHEN invoke THEN returns actual status`() = runTest {
        // Arrange
        coEvery { moneyAccountStatusFetcher(userWalletId) } returns Unit.right()
        every { moneyAccountStatusSupplier(userWalletId) } returns flowOf(
            AccountStatus.Money(account = mockk<Account.Money>(), value = MoneyAccountStatusValue.NotCreated),
        )

        // Act
        val result = useCase(userWalletId)

        // Assert
        assertThat(result).isEqualTo(MoneyAccountStatusValue.NotCreated.right())
    }

    @Test
    fun `GIVEN fetch fails WHEN invoke THEN returns error without reading status`() = runTest {
        // Arrange
        val error = IllegalStateException()
        coEvery { moneyAccountStatusFetcher(userWalletId) } returns error.left()

        // Act
        val result = useCase(userWalletId)

        // Assert
        assertThat(result).isEqualTo(error.left())
        coVerify(exactly = 0) { moneyAccountStatusSupplier(any<UserWalletId>()) }
    }
}