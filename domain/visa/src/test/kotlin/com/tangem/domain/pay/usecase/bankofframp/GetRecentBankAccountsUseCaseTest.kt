package com.tangem.domain.pay.usecase.bankofframp

import arrow.core.left
import arrow.core.right
import com.google.common.truth.Truth.assertThat
import com.tangem.domain.pay.model.bankofframp.BankOfframpError
import com.tangem.domain.pay.model.bankofframp.RecentBankAccounts
import com.tangem.domain.pay.repository.BankOfframpRepository
import com.tangem.test.core.ProvideTestModels
import io.mockk.clearMocks
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.params.ParameterizedTest

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class GetRecentBankAccountsUseCaseTest {

    private val repository: BankOfframpRepository = mockk()
    private val useCase = GetRecentBankAccountsUseCase(repository = repository)

    @BeforeEach
    fun resetMocks() {
        clearMocks(repository)
    }

    @Test
    fun `GIVEN saved accounts WHEN invoke THEN first three returned in backend order`() = runTest {
        // Arrange
        val accounts = listOf("Dave", "charlie", "Bob", "alice").map { bankAccount(id = it, displayName = it) }
        coEvery { repository.getBankAccounts(TEST_USER_WALLET_ID) } returns accounts.right()

        // Act
        val actual = useCase(TEST_USER_WALLET_ID)

        // Assert
        val expected = RecentBankAccounts(
            recents = listOf("Dave", "charlie", "Bob").map { bankAccount(id = it, displayName = it) },
            hasMoreAccounts = true,
        )
        assertThat(actual).isEqualTo(expected.right())
        coVerify(exactly = 1) { repository.getBankAccounts(TEST_USER_WALLET_ID) }
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

    @ParameterizedTest
    @ProvideTestModels
    fun `GIVEN saved accounts count WHEN invoke THEN more accounts flagged only above three`(model: CountModel) =
        runTest {
            // Arrange
            val accounts = List(model.count) { bankAccount(id = "acc-$it", displayName = "acc-$it") }
            coEvery { repository.getBankAccounts(TEST_USER_WALLET_ID) } returns accounts.right()

            // Act
            val actual = useCase(TEST_USER_WALLET_ID)

            // Assert
            val expected = RecentBankAccounts(
                recents = accounts.take(model.expectedShown),
                hasMoreAccounts = model.expectedHasMore,
            )
            assertThat(actual).isEqualTo(expected.right())
        }

    internal data class CountModel(val count: Int, val expectedShown: Int, val expectedHasMore: Boolean)

    private fun provideTestModels() = listOf(
        CountModel(count = 0, expectedShown = 0, expectedHasMore = false),
        CountModel(count = 1, expectedShown = 1, expectedHasMore = false),
        CountModel(count = 3, expectedShown = 3, expectedHasMore = false),
        CountModel(count = 4, expectedShown = 3, expectedHasMore = true),
    )
}