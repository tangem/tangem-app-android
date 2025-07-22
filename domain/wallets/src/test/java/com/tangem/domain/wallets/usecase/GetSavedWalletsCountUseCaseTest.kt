package com.tangem.domain.wallets.usecase

import com.google.common.truth.Truth.assertThat
import com.tangem.domain.wallets.legacy.UserWalletsListManager
import com.tangem.domain.wallets.legacy.asLockable
import com.tangem.domain.wallets.legacy.isLockedSync
import com.tangem.domain.wallets.models.UserWallet
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkStatic
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Test

class GetSavedWalletsCountUseCaseTest {

    private lateinit var useCase: GetSavedWalletsCountUseCase
    private lateinit var userWalletsListManager: UserWalletsListManager

    @Before
    fun setup() {
        userWalletsListManager = mockk()
        useCase = GetSavedWalletsCountUseCase(userWalletsListManager)
        mockkStatic("com.tangem.domain.wallets.legacy.UserWalletsListManagerExtensionsKt")
    }

    @Test
    fun `GIVEN manager is not lockable WHEN invoke THEN return empty list`() = runTest {
        // GIVEN
        every { userWalletsListManager.isLockable } returns false
        every { userWalletsListManager.savedWalletsCount } returns flowOf(0)
        every { userWalletsListManager.userWallets } returns flowOf(emptyList())
        every { userWalletsListManager.userWalletsSync } returns emptyList()
        every { userWalletsListManager.isLockedSync } returns false
        every { userWalletsListManager.asLockable() } returns null

        // WHEN
        val result = useCase().firstOrNull()

        // THEN
        assertThat(result).isEmpty()
    }

    @Test
    fun `GIVEN manager is locked WHEN invoke THEN return empty list`() = runTest {
        // GIVEN
        val mockLockable = mockk<UserWalletsListManager.Lockable>()
        every { userWalletsListManager.isLockable } returns true
        every { userWalletsListManager.savedWalletsCount } returns flowOf(0)
        every { userWalletsListManager.userWallets } returns flowOf(emptyList())
        every { userWalletsListManager.userWalletsSync } returns emptyList()
        every { userWalletsListManager.isLockedSync } returns true
        every { userWalletsListManager.asLockable() } returns mockLockable

        // WHEN
        val result = useCase().firstOrNull()

        // THEN
        assertThat(result).isEmpty()
    }

    @Test
    fun `GIVEN manager is not locked and has wallets WHEN invoke THEN return wallets list`() = runTest {
        // GIVEN
        val mockLockable = mockk<UserWalletsListManager.Lockable>()
        val wallets = listOf(mockk<UserWallet>(), mockk<UserWallet>())
        every { userWalletsListManager.isLockable } returns true
        every { userWalletsListManager.savedWalletsCount } returns flowOf(wallets.size)
        every { userWalletsListManager.userWallets } returns flowOf(wallets)
        every { userWalletsListManager.userWalletsSync } returns wallets
        every { userWalletsListManager.isLockedSync } returns false
        every { userWalletsListManager.asLockable() } returns mockLockable

        // WHEN
        val result = useCase().firstOrNull()

        // THEN
        assertThat(result).isEqualTo(wallets)
    }

    @Test
    fun `GIVEN manager is not lockable and savedWalletsCount is zero WHEN invoke THEN return empty list`() = runTest {
        // GIVEN
        every { userWalletsListManager.isLockable } returns false
        every { userWalletsListManager.savedWalletsCount } returns flowOf(0)
        every { userWalletsListManager.userWallets } returns flowOf(emptyList())
        every { userWalletsListManager.userWalletsSync } returns emptyList()
        every { userWalletsListManager.isLockedSync } returns false
        every { userWalletsListManager.asLockable() } returns null

        // WHEN
        val result = useCase().firstOrNull()

        // THEN
        assertThat(result).isEmpty()
    }

    @Test
    fun `GIVEN manager is lockable and locked and savedWalletsCount is zero WHEN invoke THEN return empty list`() =
        runTest {
            // GIVEN
            val mockLockable = mockk<UserWalletsListManager.Lockable>()
            every { userWalletsListManager.isLockable } returns true
            every { userWalletsListManager.savedWalletsCount } returns flowOf(0)
            every { userWalletsListManager.userWallets } returns flowOf(emptyList())
            every { userWalletsListManager.userWalletsSync } returns emptyList()
            every { userWalletsListManager.isLockedSync } returns true
            every { userWalletsListManager.asLockable() } returns mockLockable

            // WHEN
            val result = useCase().firstOrNull()

            // THEN
            assertThat(result).isEmpty()
        }

    @Test
    fun `GIVEN manager is lockable and unlocked and savedWalletsCount is zero WHEN invoke THEN return empty list`() =
        runTest {
            // GIVEN
            val mockLockable = mockk<UserWalletsListManager.Lockable>()
            every { userWalletsListManager.isLockable } returns true
            every { userWalletsListManager.savedWalletsCount } returns flowOf(0)
            every { userWalletsListManager.userWallets } returns flowOf(emptyList())
            every { userWalletsListManager.userWalletsSync } returns emptyList()
            every { userWalletsListManager.isLockedSync } returns false
            every { userWalletsListManager.asLockable() } returns mockLockable

            // WHEN
            val result = useCase().firstOrNull()

            // THEN
            assertThat(result).isEmpty()
        }
}