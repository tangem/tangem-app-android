package com.tangem.domain.pay.usecase.bankofframp

import arrow.core.left
import arrow.core.right
import com.google.common.truth.Truth.assertThat
import com.tangem.domain.pay.model.bankofframp.BankOfframpError
import com.tangem.domain.pay.repository.BankOfframpRepository
import io.mockk.clearMocks
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

internal class GetBankAccountsUseCaseTest {

    private val repository: BankOfframpRepository = mockk()
    private val useCase = GetBankAccountsUseCase(repository)

    @BeforeEach
    fun resetMocks() {
        clearMocks(repository)
    }

    @Test
    fun `GIVEN unsorted accounts WHEN invoke THEN sorted by display name ignoring case`() = runTest {
        // Arrange
        val accounts = listOf(
            bankAccount(id = "3", displayName = "saburo Arasaka"),
            bankAccount(id = "1", displayName = "Artem Toshmukhamedov"),
            bankAccount(id = "2", displayName = "Basit Didenko"),
        )
        coEvery { repository.getBankAccounts(TEST_USER_WALLET_ID) } returns accounts.right()

        // Act
        val actual = useCase(TEST_USER_WALLET_ID)

        // Assert
        assertThat(actual.getOrNull()?.map { it.id }).containsExactly("1", "2", "3").inOrder()
    }

    @Test
    fun `GIVEN repository error WHEN invoke THEN error is returned`() = runTest {
        // Arrange
        coEvery { repository.getBankAccounts(TEST_USER_WALLET_ID) } returns BankOfframpError.Unknown(cause = null).left()

        // Act
        val actual = useCase(TEST_USER_WALLET_ID)

        // Assert
        assertThat(actual).isEqualTo(BankOfframpError.Unknown(cause = null).left())
    }
}