package com.tangem.domain.wallets.usecase

import arrow.core.left
import arrow.core.right
import com.google.common.truth.Truth.assertThat
import com.tangem.domain.models.wallet.UserWallet
import com.tangem.domain.wallets.registration.WalletDeviceLinkRepository
import com.tangem.domain.wallets.registration.WalletLinkError
import io.mockk.clearMocks
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

internal class LinkWalletToDeviceUseCaseTest {

    private val walletDeviceLinkRepository: WalletDeviceLinkRepository = mockk()
    private val useCase = LinkWalletToDeviceUseCase(walletDeviceLinkRepository)

    private val userWallet: UserWallet = mockk()

    @BeforeEach
    fun setup() {
        clearMocks(walletDeviceLinkRepository)
    }

    @Test
    fun `GIVEN repository links the wallet WHEN invoke THEN returns right`() = runTest {
        // Arrange
        coEvery { walletDeviceLinkRepository.link(userWallet) } returns Unit.right()

        // Act
        val actual = useCase(userWallet)

        // Assert
        assertThat(actual).isEqualTo(Unit.right())
        coVerify(exactly = 1) { walletDeviceLinkRepository.link(userWallet) }
    }

    @Test
    fun `GIVEN repository fails WHEN invoke THEN returns the repository error`() = runTest {
        // Arrange
        coEvery { walletDeviceLinkRepository.link(userWallet) } returns WalletLinkError.Cancelled.left()

        // Act
        val actual = useCase(userWallet)

        // Assert
        assertThat(actual).isEqualTo(WalletLinkError.Cancelled.left())
    }
}