package com.tangem.domain.account.usecase

import com.google.common.truth.Truth
import com.tangem.domain.account.models.ArchivedAccount
import com.tangem.domain.account.repository.AccountsCRUDRepository
import com.tangem.domain.core.utils.lceContent
import com.tangem.domain.core.utils.lceError
import com.tangem.domain.core.utils.lceLoading
import com.tangem.domain.models.wallet.UserWalletId
import io.mockk.*
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance

@Suppress("UnusedFlow")
@OptIn(ExperimentalCoroutinesApi::class)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class GetArchivedAccountsUseCaseTest {

    private val crudRepository: AccountsCRUDRepository = mockk(relaxUnitFun = true)
    private val useCase = GetArchivedAccountsUseCase(crudRepository)
    private val userWalletId = UserWalletId("011")

    @BeforeEach
    fun resetMocks() {
        clearMocks(crudRepository)
    }

    @Test
    fun `invoke should emit archived accounts when repository returns data`() = runTest {
        // Arrange
        val archivedAccounts = listOf(
            mockk<ArchivedAccount>(),
            mockk<ArchivedAccount>(),
        )

        every { crudRepository.getArchivedAccounts(userWalletId) } returns flowOf(archivedAccounts)

        // Act
        val actual = getEmittedValues(useCase(userWalletId))

        // Assert
        val expected = listOf(
            lceLoading(),
            archivedAccounts.lceContent(),
        )
        Truth.assertThat(actual).isEqualTo(expected)

        coVerifyOrder {
            crudRepository.fetchArchivedAccounts(userWalletId)
            crudRepository.getArchivedAccounts(userWalletId)
        }
    }

    @Test
    fun `invoke should emit error if fetchArchivedAccounts throws exception`() = runTest {
        // Arrange
        val exception = IllegalStateException("Test error")
        val archivedAccounts = listOf(
            mockk<ArchivedAccount>(),
            mockk<ArchivedAccount>(),
        )

        coEvery { crudRepository.fetchArchivedAccounts(userWalletId) } throws exception
        every { crudRepository.getArchivedAccounts(userWalletId) } returns flowOf(archivedAccounts)

        // Act
        val actual = getEmittedValues(useCase(userWalletId))

        // Assert
        val expected = listOf(
            lceLoading(),
            exception.lceError(),
        )
        Truth.assertThat(actual).isEqualTo(expected)

        coVerifyOrder { crudRepository.fetchArchivedAccounts(userWalletId) }
        coVerify(inverse = true) { crudRepository.getArchivedAccounts(any()) }
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    fun <T> TestScope.getEmittedValues(flow: Flow<T>): List<T> {
        val values = mutableListOf<T>()

        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            flow.toList(values)
        }

        return values
    }
}