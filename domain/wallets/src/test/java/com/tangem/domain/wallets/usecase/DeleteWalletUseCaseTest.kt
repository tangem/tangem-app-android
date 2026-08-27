package com.tangem.domain.wallets.usecase

import arrow.core.left
import arrow.core.right
import com.google.common.truth.Truth.assertThat
import com.tangem.common.test.domain.wallet.MockUserWalletFactory
import com.tangem.domain.card.DeleteSavedAccessCodesUseCase
import com.tangem.domain.common.wallets.UserWalletDataCleaner
import com.tangem.domain.common.wallets.UserWalletsListRepository
import com.tangem.domain.common.wallets.error.DeleteWalletError
import com.tangem.domain.models.wallet.UserWallet
import com.tangem.domain.models.wallet.UserWalletId
import com.tangem.test.core.TestAppCoroutineScope
import io.mockk.Runs
import io.mockk.clearMocks
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class DeleteWalletUseCaseTest {

    private val userWalletsListRepository: UserWalletsListRepository = mockk(relaxed = true)
    private val firstCleaner: UserWalletDataCleaner = mockk()
    private val secondCleaner: UserWalletDataCleaner = mockk()
    private val deleteSavedAccessCodesUseCase: DeleteSavedAccessCodesUseCase = mockk()

    @BeforeEach
    fun resetMocks() {
        clearMocks(userWalletsListRepository, firstCleaner, secondCleaner, deleteSavedAccessCodesUseCase)
        coEvery { userWalletsListRepository.delete(any()) } returns Unit.right()
        coEvery { userWalletsListRepository.userWalletsSync() } returns emptyList()
        every { userWalletsListRepository.selectedUserWallet } returns MutableStateFlow<UserWallet?>(null)
        coEvery { firstCleaner.clear(any()) } just Runs
        coEvery { secondCleaner.clear(any()) } just Runs
        coEvery { deleteSavedAccessCodesUseCase(any<Set<String>>()) } returns Unit.right()
    }

    @Test
    fun `GIVEN delete succeeds WHEN invoke THEN cleanup runs in background after invoke returns`() = runTest {
        // Arrange
        val useCase = createUseCase()

        // Act
        useCase(WALLET_ID)

        // Assert — cleanup is fire-and-forget: not run yet when invoke returns
        coVerify(exactly = 0) { firstCleaner.clear(any()) }
        coVerify(exactly = 0) { secondCleaner.clear(any()) }

        // ...and completes for every wallet once the app scope is drained
        advanceUntilIdle()
        coVerify(exactly = 1) { firstCleaner.clear(listOf(WALLET_ID)) }
        coVerify(exactly = 1) { secondCleaner.clear(listOf(WALLET_ID)) }
    }

    @Test
    fun `GIVEN delete fails WHEN invoke THEN cleaners are not run and error returned`() = runTest {
        // Arrange
        coEvery { userWalletsListRepository.delete(any()) } returns DeleteWalletError.UnableToDelete.left()
        val useCase = createUseCase()

        // Act
        val result = useCase(WALLET_ID)
        advanceUntilIdle()

        // Assert
        assertThat(result).isEqualTo(DeleteWalletError.UnableToDelete.left())
        coVerify(exactly = 0) { firstCleaner.clear(any()) }
        coVerify(exactly = 0) { secondCleaner.clear(any()) }
        coVerify(exactly = 0) { deleteSavedAccessCodesUseCase(any<Set<String>>()) }
    }

    @Test
    fun `GIVEN a wallet still selected WHEN invoke THEN returns right true`() = runTest {
        // Arrange
        every { userWalletsListRepository.selectedUserWallet } returns MutableStateFlow(wallet)
        val useCase = createUseCase()

        // Act
        val result = useCase(WALLET_ID)

        // Assert
        assertThat(result).isEqualTo(true.right())
    }

    @Test
    fun `GIVEN no wallet selected WHEN invoke THEN returns right false`() = runTest {
        // Arrange
        val useCase = createUseCase()

        // Act
        val result = useCase(WALLET_ID)

        // Assert
        assertThat(result).isEqualTo(false.right())
    }

    @Test
    fun `GIVEN one cleaner throws WHEN invoke THEN other cleaners still run and result is right`() = runTest {
        // Arrange
        coEvery { firstCleaner.clear(any()) } throws IllegalStateException("boom")
        val useCase = createUseCase()

        // Act
        val result = useCase(WALLET_ID)
        advanceUntilIdle()

        // Assert
        assertThat(result).isEqualTo(false.right())
        coVerify(exactly = 1) { secondCleaner.clear(listOf(WALLET_ID)) }
    }

    @Test
    fun `GIVEN cold wallet with cards WHEN invoke THEN saved access codes deleted for all wallet cards`() = runTest {
        // Arrange
        val coldWallet = wallet.copy(walletId = WALLET_ID, cardsInWallet = setOf("CB01", "CB02"))
        coEvery { userWalletsListRepository.userWalletsSync() } returns listOf(coldWallet)
        val useCase = createUseCase()

        // Act
        useCase(WALLET_ID)
        advanceUntilIdle()

        // Assert
        coVerify(exactly = 1) { deleteSavedAccessCodesUseCase(setOf("CB01", "CB02", coldWallet.cardId)) }
    }

    @Test
    fun `GIVEN wallet not found WHEN invoke THEN saved access codes deletion is not invoked`() = runTest {
        // Arrange
        val useCase = createUseCase()

        // Act
        useCase(WALLET_ID)
        advanceUntilIdle()

        // Assert
        coVerify(exactly = 0) { deleteSavedAccessCodesUseCase(any<Set<String>>()) }
    }

    @Test
    fun `GIVEN access codes deletion throws WHEN invoke THEN cleaners still run`() = runTest {
        // Arrange
        val coldWallet = wallet.copy(walletId = WALLET_ID, cardsInWallet = setOf("CB01"))
        coEvery { userWalletsListRepository.userWalletsSync() } returns listOf(coldWallet)
        coEvery { deleteSavedAccessCodesUseCase(any<Set<String>>()) } throws IllegalStateException("boom")
        val useCase = createUseCase()

        // Act
        val result = useCase(WALLET_ID)
        advanceUntilIdle()

        // Assert
        assertThat(result).isEqualTo(false.right())
        coVerify(exactly = 1) { firstCleaner.clear(listOf(WALLET_ID)) }
        coVerify(exactly = 1) { secondCleaner.clear(listOf(WALLET_ID)) }
    }

    private fun TestScope.createUseCase() = DeleteWalletUseCase(
        userWalletsListRepository = userWalletsListRepository,
        userWalletDataCleaners = setOf(firstCleaner, secondCleaner),
        deleteSavedAccessCodesUseCase = deleteSavedAccessCodesUseCase,
        appCoroutineScope = TestAppCoroutineScope(this),
    )

    private companion object {
        val WALLET_ID = UserWalletId("0011")
        val wallet: UserWallet.Cold = MockUserWalletFactory.create()
    }
}