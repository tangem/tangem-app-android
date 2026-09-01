package com.tangem.domain.wallets.usecase

import arrow.core.Either
import com.google.common.truth.Truth.assertThat
import com.tangem.domain.common.wallets.UserWalletsListRepository
import com.tangem.domain.common.wallets.error.SelectWalletError
import com.tangem.domain.models.wallet.UserWallet
import com.tangem.domain.models.wallet.UserWalletId
import com.tangem.domain.models.wallet.isLocked
import com.tangem.domain.wallets.models.errors.GetUserWalletError
import io.mockk.clearMocks
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class ResolveAndSelectUserWalletUseCaseTest {

    private val getUserWalletUseCase: GetUserWalletUseCase = mockk()
    private val userWalletsListRepository: UserWalletsListRepository = mockk()

    private val useCase = ResolveAndSelectUserWalletUseCase(
        getUserWalletUseCase = getUserWalletUseCase,
        userWalletsListRepository = userWalletsListRepository,
    )

    private val userWalletId = UserWalletId("011")

    @BeforeEach
    fun resetMocks() {
        clearMocks(getUserWalletUseCase, userWalletsListRepository)
    }

    @Test
    fun `GIVEN no wallet id WHEN invoke THEN returns selected wallet`() = runTest {
        // Arrange
        val selectedWallet: UserWallet = mockk {
            every { walletId } returns userWalletId
            every { isLocked } returns false
        }
        coEvery { userWalletsListRepository.selectedUserWalletSync() } returns selectedWallet

        // Act
        val result = useCase(userWalletId = null)

        // Assert
        assertThat(result).isEqualTo(selectedWallet)
        coVerify(exactly = 0) { userWalletsListRepository.select(any()) }
    }

    @Test
    fun `GIVEN wallet id matches selected wallet WHEN invoke THEN returns wallet without reselecting or refetching`() =
        runTest {
            // Arrange
            val wallet: UserWallet = mockk {
                every { walletId } returns userWalletId
                every { isLocked } returns false
            }
            coEvery { userWalletsListRepository.selectedUserWalletSync() } returns wallet

            // Act
            val result = useCase(userWalletId)

            // Assert
            assertThat(result).isEqualTo(wallet)
            verify(exactly = 0) { getUserWalletUseCase(any()) }
            coVerify(exactly = 0) { userWalletsListRepository.select(any()) }
        }

    @Test
    fun `GIVEN wallet id differs from selected wallet WHEN invoke THEN selects wallet`() = runTest {
        // Arrange
        val otherWalletId = UserWalletId("022")
        val selectedWallet: UserWallet = mockk { every { walletId } returns otherWalletId }
        val targetWallet: UserWallet = mockk {
            every { walletId } returns userWalletId
            every { isLocked } returns false
        }
        coEvery { userWalletsListRepository.selectedUserWalletSync() } returns selectedWallet
        every { getUserWalletUseCase(userWalletId) } returns Either.Right(targetWallet)
        coEvery { userWalletsListRepository.select(userWalletId) } returns Either.Right(targetWallet)

        // Act
        val result = useCase(userWalletId)

        // Assert
        assertThat(result).isEqualTo(targetWallet)
        coVerify(exactly = 1) { userWalletsListRepository.select(userWalletId) }
    }

    @Test
    fun `GIVEN user wallet not found WHEN invoke THEN returns null`() = runTest {
        // Arrange
        coEvery { userWalletsListRepository.selectedUserWalletSync() } returns null
        every { getUserWalletUseCase(userWalletId) } returns Either.Left(GetUserWalletError.UserWalletNotFound)

        // Act
        val result = useCase(userWalletId)

        // Assert
        assertThat(result).isNull()
    }

    @Test
    fun `GIVEN user wallet is locked WHEN invoke THEN returns null`() = runTest {
        // Arrange
        val lockedWallet: UserWallet = mockk {
            every { walletId } returns userWalletId
            every { isLocked } returns true
        }
        coEvery { userWalletsListRepository.selectedUserWalletSync() } returns null
        every { getUserWalletUseCase(userWalletId) } returns Either.Right(lockedWallet)

        // Act
        val result = useCase(userWalletId)

        // Assert
        assertThat(result).isNull()
    }

    @Test
    fun `GIVEN wallet selection fails WHEN invoke THEN returns null`() = runTest {
        // Arrange
        val otherWalletId = UserWalletId("022")
        val selectedWallet: UserWallet = mockk { every { walletId } returns otherWalletId }
        val targetWallet: UserWallet = mockk {
            every { walletId } returns userWalletId
            every { isLocked } returns false
        }
        coEvery { userWalletsListRepository.selectedUserWalletSync() } returns selectedWallet
        every { getUserWalletUseCase(userWalletId) } returns Either.Right(targetWallet)
        coEvery { userWalletsListRepository.select(userWalletId) } returns Either.Left(SelectWalletError.UnableToSelectUserWallet)

        // Act
        val result = useCase(userWalletId)

        // Assert
        assertThat(result).isNull()
    }
}