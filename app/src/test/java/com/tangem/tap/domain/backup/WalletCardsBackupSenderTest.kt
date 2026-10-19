package com.tangem.tap.domain.backup

import arrow.core.left
import arrow.core.right
import com.tangem.core.local.connection.NetworkConnectionManager
import com.tangem.domain.common.wallets.UserWalletsListRepository
import com.tangem.domain.models.wallet.UserWallet
import com.tangem.domain.models.wallet.UserWalletId
import com.tangem.domain.wallets.models.errors.WalletCardsBackupError
import com.tangem.domain.wallets.usecase.SendPendingWalletCardsBackupUseCase
import com.tangem.features.onboarding.v2.OnboardingV2FeatureToggles
import com.tangem.test.core.TestAppCoroutineScope
import io.mockk.clearMocks
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

internal class WalletCardsBackupSenderTest {

    private val sendPendingWalletCardsBackupUseCase: SendPendingWalletCardsBackupUseCase = mockk()
    private val networkConnectionManager: NetworkConnectionManager = mockk()
    private val userWalletsListRepository: UserWalletsListRepository = mockk()
    private val onboardingV2FeatureToggles: OnboardingV2FeatureToggles = mockk()

    private val isOnline = MutableStateFlow(false)
    private val userWallets = MutableStateFlow<List<UserWallet>?>(null)

    @BeforeEach
    fun resetMocks() {
        clearMocks(
            sendPendingWalletCardsBackupUseCase,
            networkConnectionManager,
            userWalletsListRepository,
            onboardingV2FeatureToggles,
        )
        isOnline.value = false
        userWallets.value = null

        every { onboardingV2FeatureToggles.isCardLinkedStatusUpdateEnabled } returns true
        every { networkConnectionManager.isOnlineFlow } returns isOnline
        every { userWalletsListRepository.userWallets } returns userWallets
        coEvery { sendPendingWalletCardsBackupUseCase() } returns Unit.right()
    }

    @Test
    fun `GIVEN toggle disabled WHEN started online THEN sends nothing`() = runTest {
        // Arrange
        every { onboardingV2FeatureToggles.isCardLinkedStatusUpdateEnabled } returns false
        isOnline.value = true

        // Act
        createSender().start()
        runCurrent()

        // Assert
        coVerify(exactly = 0) { sendPendingWalletCardsBackupUseCase() }
    }

    @Test
    fun `GIVEN online WHEN started THEN sends pending reports`() = runTest {
        // Arrange
        isOnline.value = true

        // Act
        createSender().start()
        runCurrent()

        // Assert
        coVerify(exactly = 1) { sendPendingWalletCardsBackupUseCase() }
    }

    @Test
    fun `GIVEN offline WHEN started THEN sends nothing`() = runTest {
        // Act
        createSender().start()
        runCurrent()

        // Assert
        coVerify(exactly = 0) { sendPendingWalletCardsBackupUseCase() }
    }

    @Test
    fun `GIVEN offline WHEN connection comes back THEN sends pending reports`() = runTest {
        // Arrange
        createSender().start()
        runCurrent()

        // Act
        isOnline.value = true
        runCurrent()

        // Assert
        coVerify(exactly = 1) { sendPendingWalletCardsBackupUseCase() }
    }

    @Test
    fun `GIVEN online WHEN connection drops and comes back THEN sends again`() = runTest {
        // Arrange
        isOnline.value = true
        createSender().start()
        runCurrent()

        // Act
        isOnline.value = false
        runCurrent()
        isOnline.value = true
        runCurrent()

        // Assert
        coVerify(exactly = 2) { sendPendingWalletCardsBackupUseCase() }
    }

    @Test
    fun `GIVEN locked wallet WHEN it is unlocked THEN sends pending reports`() = runTest {
        // Arrange
        userWallets.value = listOf(createWallet(id = "1", isLocked = true))
        createSender().start()
        runCurrent()

        // Act
        userWallets.value = listOf(createWallet(id = "1", isLocked = false))
        runCurrent()

        // Assert
        coVerify(exactly = 1) { sendPendingWalletCardsBackupUseCase() }
    }

    @Test
    fun `GIVEN unlocked wallet WHEN wallets list changes without a new unlock THEN sends nothing more`() = runTest {
        // Arrange
        val unlockedWallet = createWallet(id = "1", isLocked = false)
        userWallets.value = listOf(unlockedWallet)
        createSender().start()
        runCurrent()

        // Act
        userWallets.value = listOf(unlockedWallet, createWallet(id = "2", isLocked = true))
        runCurrent()

        // Assert
        coVerify(exactly = 1) { sendPendingWalletCardsBackupUseCase() }
    }

    @Test
    fun `GIVEN sending failed WHEN connection comes back THEN sends again`() = runTest {
        // Arrange
        coEvery { sendPendingWalletCardsBackupUseCase() } returns WalletCardsBackupError.NoInternetConnection.left()
        isOnline.value = true
        createSender().start()
        runCurrent()

        // Act
        isOnline.value = false
        runCurrent()
        isOnline.value = true
        runCurrent()

        // Assert
        coVerify(exactly = 2) { sendPendingWalletCardsBackupUseCase() }
    }

    @Test
    fun `GIVEN started WHEN started again THEN observes once`() = runTest {
        // Arrange
        val sender = createSender()
        sender.start()
        runCurrent()

        // Act
        sender.start()
        isOnline.value = true
        runCurrent()

        // Assert
        coVerify(exactly = 1) { sendPendingWalletCardsBackupUseCase() }
    }

    private fun TestScope.createSender() = WalletCardsBackupSender(
        sendPendingWalletCardsBackupUseCase = sendPendingWalletCardsBackupUseCase,
        networkConnectionManager = networkConnectionManager,
        userWalletsListRepository = userWalletsListRepository,
        onboardingV2FeatureToggles = onboardingV2FeatureToggles,
        appScope = TestAppCoroutineScope(backgroundScope.coroutineContext),
    )

    private fun createWallet(id: String, isLocked: Boolean): UserWallet.Cold = mockk {
        every { walletId } returns UserWalletId(id)
        every { this@mockk.isLocked } returns isLocked
    }
}