package com.tangem.domain.wallets.usecase

import arrow.core.right
import com.google.common.truth.Truth.assertThat
import com.tangem.common.test.domain.wallet.MockUserWalletFactory
import com.tangem.core.analytics.api.AnalyticsEventHandler
import com.tangem.domain.common.wallets.UserWalletsListRepository
import com.tangem.domain.models.wallet.UserWallet
import com.tangem.domain.models.wallet.UserWalletId
import com.tangem.domain.wallets.registration.WalletRegistrationTrigger
import com.tangem.domain.wallets.repository.WalletsRepository
import io.mockk.clearMocks
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class SaveWalletUseCaseTest {

    private val userWalletsListRepository: UserWalletsListRepository = mockk(relaxed = true)
    private val walletsRepository: WalletsRepository = mockk(relaxed = true)
    private val analyticsEventHandler: AnalyticsEventHandler = mockk(relaxed = true)
    private val walletRegistrationTrigger: WalletRegistrationTrigger = mockk(relaxed = true)

    private val useCase = SaveWalletUseCase(
        userWalletsListRepository = userWalletsListRepository,
        walletsRepository = walletsRepository,
        analyticsEventHandler = analyticsEventHandler,
        walletRegistrationTrigger = walletRegistrationTrigger,
    )

    @BeforeEach
    fun setup() {
        clearMocks(userWalletsListRepository, walletsRepository, analyticsEventHandler, walletRegistrationTrigger)
        coEvery { userWalletsListRepository.setLock(any(), any(), any()) } returns Unit.right()
        coEvery { walletsRepository.useBiometricAuthentication() } returns false
    }

    @Test
    fun `GIVEN new hot wallet WHEN save THEN registration trigger fired once`() = runTest {
        // Arrange
        val wallet = hotWallet()
        stubSaveReturns(wallet)
        coEvery { userWalletsListRepository.userWalletsSync() } returns emptyList()

        // Act
        val result = useCase(wallet)

        // Assert
        assertThat(result.isRight()).isTrue()
        verify(exactly = 1) { walletRegistrationTrigger.onMobileWalletCreated(wallet) }
    }

    @Test
    fun `GIVEN new cold wallet WHEN save THEN registration trigger not fired`() = runTest {
        // Arrange
        val wallet = MockUserWalletFactory.create()
        stubSaveReturns(wallet)
        coEvery { userWalletsListRepository.userWalletsSync() } returns emptyList()

        // Act
        val result = useCase(wallet)

        // Assert
        assertThat(result.isRight()).isTrue()
        verify(exactly = 0) { walletRegistrationTrigger.onMobileWalletCreated(any()) }
    }

    @Test
    fun `GIVEN already saved hot wallet WHEN save THEN registration trigger not fired`() = runTest {
        // Arrange
        val wallet = hotWallet()
        stubSaveReturns(wallet)
        coEvery { userWalletsListRepository.userWalletsSync() } returns listOf(wallet)

        // Act
        val result = useCase(wallet, canOverride = true)

        // Assert
        assertThat(result.isRight()).isTrue()
        verify(exactly = 0) { walletRegistrationTrigger.onMobileWalletCreated(any()) }
    }

    private fun stubSaveReturns(wallet: UserWallet) {
        coEvery { userWalletsListRepository.saveWithoutLock(any(), any()) } returns wallet.right()
        coEvery { userWalletsListRepository.select(any()) } returns wallet.right()
    }

    private fun hotWallet(walletIdValue: ByteArray = ByteArray(32) { 1 }): UserWallet.Hot {
        val wallet = mockk<UserWallet.Hot>()
        every { wallet.walletId } returns UserWalletId(value = walletIdValue)
        return wallet
    }
}