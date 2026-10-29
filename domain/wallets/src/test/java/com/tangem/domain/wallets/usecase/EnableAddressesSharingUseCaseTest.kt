package com.tangem.domain.wallets.usecase

import com.tangem.domain.account.repository.AccountsCRUDRepository
import com.tangem.domain.models.wallet.UserWalletId
import com.tangem.domain.wallets.repository.WalletsRepository
import io.mockk.clearMocks
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.coVerifyOrder
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class EnableAddressesSharingUseCaseTest {

    private val walletsRepository: WalletsRepository = mockk()
    private val accountsCRUDRepository: AccountsCRUDRepository = mockk()

    private val useCase = EnableAddressesSharingUseCase(
        walletsRepository = walletsRepository,
        accountsCRUDRepository = accountsCRUDRepository,
    )

    private val userWalletId = UserWalletId("1234567890abcdef")

    @BeforeEach
    fun resetMocks() {
        clearMocks(walletsRepository, accountsCRUDRepository)
    }

    @Test
    fun `GIVEN sharing not required yet WHEN invoke THEN flag is set and tokens are synced`() = runTest {
        // Arrange
        coEvery { walletsRepository.isAddressesSharingRequired(userWalletId) } returns false
        coEvery { walletsRepository.setAddressesSharingRequired(userWalletId) } returns Unit
        coEvery { accountsCRUDRepository.syncTokens(userWalletId) } returns Unit

        // Act
        useCase(userWalletId)

        // Assert
        coVerifyOrder {
            walletsRepository.setAddressesSharingRequired(userWalletId)
            accountsCRUDRepository.syncTokens(userWalletId)
        }
    }

    @Test
    fun `GIVEN sharing already required WHEN invoke THEN nothing is sent again`() = runTest {
        // Arrange
        coEvery { walletsRepository.isAddressesSharingRequired(userWalletId) } returns true

        // Act
        useCase(userWalletId)

        // Assert
        coVerify(exactly = 0) { walletsRepository.setAddressesSharingRequired(any()) }
        coVerify(exactly = 0) { accountsCRUDRepository.syncTokens(any()) }
    }
}