package com.tangem.domain.wallets.usecase

import app.cash.turbine.test
import com.google.common.truth.Truth.assertThat
import com.tangem.common.test.domain.wallet.MockUserWalletFactory
import com.tangem.domain.common.wallets.UserWalletsListRepository
import com.tangem.domain.models.wallet.UserWallet
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Test

internal class GetWalletsUseCaseTest {

    private val userWallets = MutableStateFlow<List<UserWallet>?>(null)
    private val repository: UserWalletsListRepository = mockk {
        every { userWallets } returns this@GetWalletsUseCaseTest.userWallets
    }

    private val useCase = GetWalletsUseCase(repository)

    @Test
    fun `GIVEN wallets are not loaded WHEN invoke THEN nothing is emitted`() = runTest {
        // Arrange
        userWallets.value = null

        // Act & Assert
        useCase().test {
            expectNoEvents()
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `GIVEN wallets load while collecting WHEN invoke THEN loaded list is emitted`() = runTest {
        // Arrange
        userWallets.value = null
        val wallets = listOf(MockUserWalletFactory.create())

        // Act & Assert
        useCase().test {
            expectNoEvents()

            userWallets.value = wallets

            assertThat(awaitItem()).isEqualTo(wallets)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `GIVEN wallets are loaded WHEN invokeAsMap THEN map is emitted`() = runTest {
        // Arrange
        val wallet = MockUserWalletFactory.create()
        userWallets.value = listOf(wallet)

        // Act & Assert
        useCase.invokeAsMap().test {
            assertThat(awaitItem()).isEqualTo(linkedMapOf(wallet.walletId to wallet))
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `GIVEN wallets are not loaded WHEN invokeSync THEN throws`() {
        // Arrange
        userWallets.value = null

        // Act
        val actual = runCatching { useCase.invokeSync() }.exceptionOrNull()

        // Assert
        assertThat(actual).isInstanceOf(IllegalArgumentException::class.java)
    }
}