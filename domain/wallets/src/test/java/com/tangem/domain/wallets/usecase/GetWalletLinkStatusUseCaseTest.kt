package com.tangem.domain.wallets.usecase

import com.google.common.truth.Truth.assertThat
import com.tangem.domain.common.wallets.UserWalletsListRepository
import com.tangem.domain.models.wallet.UserWallet
import com.tangem.domain.models.wallet.UserWalletId
import com.tangem.domain.wallets.registration.WalletDeviceLinkRepository
import io.mockk.clearMocks
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.take
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import java.io.IOException

internal class GetWalletLinkStatusUseCaseTest {

    private val walletDeviceLinkRepository: WalletDeviceLinkRepository = mockk()
    private val userWalletsListRepository: UserWalletsListRepository = mockk()
    private val userWallets = MutableStateFlow<List<UserWallet>?>(null)

    private val useCase = GetWalletLinkStatusUseCase(
        walletDeviceLinkRepository = walletDeviceLinkRepository,
        userWalletsListRepository = userWalletsListRepository,
    )

    private val userWallet = mockk<UserWallet.Hot> {
        every { walletId } returns WALLET_ID
    }

    @BeforeEach
    fun setup() {
        clearMocks(walletDeviceLinkRepository, userWalletsListRepository)
        userWallets.value = listOf(userWallet)
        every { userWalletsListRepository.userWallets } returns userWallets
    }

    @Nested
    @TestInstance(TestInstance.Lifecycle.PER_CLASS)
    inner class ByWallet {

        @Test
        fun `GIVEN repository link status changes WHEN invoke THEN emits the repository statuses`() = runTest {
            // Arrange
            every { walletDeviceLinkRepository.isLinked(userWallet) } returns flowOf(false, true)

            // Act
            val actual = useCase(userWallet).toList()

            // Assert
            assertThat(actual).containsExactly(false, true).inOrder()
        }
    }

    @Nested
    @TestInstance(TestInstance.Lifecycle.PER_CLASS)
    inner class ByWalletId {

        @Test
        fun `GIVEN a known wallet WHEN invoke THEN emits its link statuses`() = runTest {
            // Arrange
            every { walletDeviceLinkRepository.isLinked(userWallet) } returns flowOf(false, false, true)

            // Act
            val actual = useCase(WALLET_ID).take(2).toList()

            // Assert
            assertThat(actual).containsExactly(false, true).inOrder()
        }

        @Test
        fun `GIVEN the link status completes empty WHEN invoke THEN true`() = runTest {
            // Arrange
            every { walletDeviceLinkRepository.isLinked(userWallet) } returns emptyFlow()

            // Act
            val actual = useCase(WALLET_ID).first()

            // Assert
            assertThat(actual).isTrue()
        }

        @Test
        fun `GIVEN the link status completes after false WHEN invoke THEN falls back to true`() = runTest {
            // Arrange
            every { walletDeviceLinkRepository.isLinked(userWallet) } returns flow {
                emit(false)
                throw IOException("registered wallet ids unreadable")
            }.catch { }

            // Act
            val actual = useCase(WALLET_ID).take(2).toList()

            // Assert
            assertThat(actual).containsExactly(false, true).inOrder()
        }

        @Test
        fun `GIVEN an unknown wallet WHEN invoke THEN true without asking the link status`() = runTest {
            // Arrange
            userWallets.value = emptyList()

            // Act
            val actual = useCase(WALLET_ID).first()

            // Assert
            assertThat(actual).isTrue()
            verify(exactly = 0) { walletDeviceLinkRepository.isLinked(any()) }
        }

        @Test
        fun `GIVEN wallets not loaded yet WHEN invoke THEN true`() = runTest {
            // Arrange
            userWallets.value = null

            // Act
            val actual = useCase(WALLET_ID).first()

            // Assert
            assertThat(actual).isTrue()
        }
    }

    private companion object {
        val WALLET_ID = UserWalletId("011")
    }
}