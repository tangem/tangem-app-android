package com.tangem.tap.domain.walletregistration

import com.tangem.domain.models.wallet.UserWallet
import com.tangem.hot.sdk.model.HotAuth
import com.tangem.test.core.TestAppCoroutineScope
import io.mockk.Runs
import io.mockk.clearMocks
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.just
import io.mockk.mockk
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class DefaultWalletRegistrationTriggerTest {

    private val launcher: WalletRegistrationLauncher = mockk()

    @BeforeEach
    fun setup() {
        // PER_CLASS shares one launcher mock across tests — reset recorded calls so the empty-list
        // test's verify(exactly = 0) can't observe calls made by an earlier test.
        clearMocks(launcher)
    }

    @Test
    fun `GIVEN hot wallet created WHEN triggered THEN registers it`() = runTest {
        // Arrange — a real app scope so the fire-and-forget registration actually runs.
        val trigger = DefaultWalletRegistrationTrigger(
            launcher = launcher,
            appCoroutineScope = TestAppCoroutineScope(this),
        )
        val wallet = mockk<UserWallet.Hot>()
        coEvery { launcher.registerMobile(wallet) } just Runs

        // Act
        trigger.onMobileWalletCreated(wallet)
        advanceUntilIdle()

        // Assert
        coVerify(exactly = 1) { launcher.registerMobile(wallet) }
    }

    @Test
    fun `GIVEN wallets unlocked WHEN triggered THEN registers each`() = runTest {
        // Arrange
        val trigger = DefaultWalletRegistrationTrigger(
            launcher = launcher,
            appCoroutineScope = TestAppCoroutineScope(this),
        )
        val walletA = mockk<UserWallet.Hot>()
        val walletB = mockk<UserWallet.Hot>()
        coEvery { launcher.registerUnlockedMobileWallet(any()) } just Runs

        // Act
        trigger.onMobileWalletsUnlocked(listOf(walletA, walletB))
        advanceUntilIdle()

        // Assert
        coVerify(exactly = 1) { launcher.registerUnlockedMobileWallet(walletA) }
        coVerify(exactly = 1) { launcher.registerUnlockedMobileWallet(walletB) }
    }

    @Test
    fun `GIVEN wallet unlocked with access code WHEN triggered THEN registers it with that code`() = runTest {
        // Arrange
        val trigger = DefaultWalletRegistrationTrigger(
            launcher = launcher,
            appCoroutineScope = TestAppCoroutineScope(this),
        )
        val wallet = mockk<UserWallet.Hot>()
        val accessCode = HotAuth.Password("111111".toCharArray())
        coEvery { launcher.registerUnlockedMobileWallet(any(), any()) } just Runs

        // Act
        trigger.onMobileWalletUnlockedWithAccessCode(wallet, accessCode)
        advanceUntilIdle()

        // Assert
        coVerify(exactly = 1) { launcher.registerUnlockedMobileWallet(wallet, accessCode) }
    }

    @Test
    fun `GIVEN empty unlocked list WHEN triggered THEN nothing registered`() = runTest {
        // Arrange
        val trigger = DefaultWalletRegistrationTrigger(
            launcher = launcher,
            appCoroutineScope = TestAppCoroutineScope(this),
        )

        // Act
        trigger.onMobileWalletsUnlocked(emptyList())
        advanceUntilIdle()

        // Assert
        coVerify(exactly = 0) { launcher.registerUnlockedMobileWallet(any()) }
    }
}