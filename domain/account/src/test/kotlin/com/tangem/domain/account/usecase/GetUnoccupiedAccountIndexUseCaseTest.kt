package com.tangem.domain.account.usecase

import arrow.core.left
import arrow.core.right
import com.google.common.truth.Truth
import com.tangem.domain.account.repository.AccountsCRUDRepository
import com.tangem.domain.models.wallet.UserWalletId
import io.mockk.clearMocks
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class GetUnoccupiedAccountIndexUseCaseTest {

    private val crudRepository: AccountsCRUDRepository = mockk(relaxUnitFun = true)
    private val useCase = GetUnoccupiedAccountIndexUseCase(crudRepository)
    private val userWalletId = UserWalletId("011")

    @BeforeEach
    fun resetMocks() {
        clearMocks(crudRepository)
    }

    @Test
    fun `invoke should return next unoccupied index when repository returns count`() = runTest {
        // Arrange
        coEvery { crudRepository.getTotalAccountsCount(userWalletId) } returns 3

        // Act
        val actual = useCase(userWalletId = userWalletId)

        // Assert
        val expected = 4.right()
        Truth.assertThat(actual).isEqualTo(expected)

        coVerify { crudRepository.getTotalAccountsCount(userWalletId) }
    }

    @Test
    fun `invoke should return error if repository throws exception`() = runTest {
        // Arrange
        val exception = IllegalStateException("Test error")
        coEvery { crudRepository.getTotalAccountsCount(userWalletId) } throws exception

        // Act
        val actual = useCase(userWalletId = userWalletId)

        // Assert
        val expected = GetUnoccupiedAccountIndexUseCase.Error.DataOperationFailed(exception).left()
        Truth.assertThat(actual).isEqualTo(expected)

        coVerify { crudRepository.getTotalAccountsCount(userWalletId) }
    }
}