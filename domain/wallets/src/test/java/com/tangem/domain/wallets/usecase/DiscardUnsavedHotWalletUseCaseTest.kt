package com.tangem.domain.wallets.usecase

import com.google.common.truth.Truth.assertThat
import com.tangem.domain.common.wallets.UserWalletsListRepository
import com.tangem.domain.models.wallet.UserWallet
import com.tangem.domain.models.wallet.UserWalletId
import com.tangem.hot.sdk.TangemHotSdk
import com.tangem.hot.sdk.model.HotWalletId
import io.mockk.clearMocks
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.runs
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

internal class DiscardUnsavedHotWalletUseCaseTest {

    private val tangemHotSdk: TangemHotSdk = mockk()
    private val userWalletsListRepository: UserWalletsListRepository = mockk()

    private val useCase = DiscardUnsavedHotWalletUseCase(tangemHotSdk, userWalletsListRepository)

    @BeforeEach
    fun resetMocks() {
        clearMocks(tangemHotSdk, userWalletsListRepository)
    }

    @Test
    fun `GIVEN hot wallet with same id listed WHEN invoke THEN sdk entry is kept`() = runTest {
        // Arrange
        every { userWalletsListRepository.userWallets } returns MutableStateFlow(listOf(hotWallet(HOT_WALLET_ID)))

        // Act
        val isListed = useCase(HOT_WALLET_ID)

        // Assert
        assertThat(isListed).isTrue()
        coVerify(exactly = 0) { tangemHotSdk.delete(any()) }
    }

    @Test
    fun `GIVEN only other wallets listed WHEN invoke THEN sdk entry is deleted`() = runTest {
        // Arrange
        val otherHotWalletId = HotWalletId(value = "other", authType = HotWalletId.AuthType.NoPassword)
        every { userWalletsListRepository.userWallets } returns MutableStateFlow(
            listOf(hotWallet(otherHotWalletId), mockk<UserWallet.Cold>()),
        )
        coEvery { tangemHotSdk.delete(HOT_WALLET_ID) } just runs

        // Act
        val isListed = useCase(HOT_WALLET_ID)

        // Assert
        assertThat(isListed).isFalse()
        coVerify(exactly = 1) { tangemHotSdk.delete(HOT_WALLET_ID) }
    }

    @Test
    fun `GIVEN wallets list not loaded WHEN invoke THEN sdk entry is deleted`() = runTest {
        // Arrange
        every { userWalletsListRepository.userWallets } returns MutableStateFlow(null)
        coEvery { tangemHotSdk.delete(HOT_WALLET_ID) } just runs

        // Act
        useCase(HOT_WALLET_ID)

        // Assert
        coVerify(exactly = 1) { tangemHotSdk.delete(HOT_WALLET_ID) }
    }

    @Test
    fun `GIVEN sdk delete throws WHEN invoke THEN error is swallowed`() = runTest {
        // Arrange
        every { userWalletsListRepository.userWallets } returns MutableStateFlow(emptyList())
        coEvery { tangemHotSdk.delete(HOT_WALLET_ID) } throws IllegalStateException("delete failed")

        // Act
        val error = runCatching { useCase(HOT_WALLET_ID) }.exceptionOrNull()

        // Assert
        assertThat(error).isNull()
        coVerify(exactly = 1) { tangemHotSdk.delete(HOT_WALLET_ID) }
    }

    private fun hotWallet(hotWalletId: HotWalletId): UserWallet.Hot = UserWallet.Hot(
        name = "Hot",
        walletId = UserWalletId("0A0B"),
        hotWalletId = hotWalletId,
        wallets = null,
        isBackedUp = false,
    )

    private companion object {
        val HOT_WALLET_ID = HotWalletId(value = "hot", authType = HotWalletId.AuthType.NoPassword)
    }
}