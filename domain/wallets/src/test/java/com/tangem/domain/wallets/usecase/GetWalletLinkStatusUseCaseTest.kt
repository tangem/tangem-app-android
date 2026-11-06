package com.tangem.domain.wallets.usecase

import com.google.common.truth.Truth.assertThat
import com.tangem.domain.models.wallet.UserWallet
import com.tangem.domain.wallets.registration.WalletDeviceLinkRepository
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Test

internal class GetWalletLinkStatusUseCaseTest {

    private val walletDeviceLinkRepository: WalletDeviceLinkRepository = mockk()
    private val useCase = GetWalletLinkStatusUseCase(walletDeviceLinkRepository)

    @Test
    fun `GIVEN repository link status changes WHEN invoke THEN emits the repository statuses`() = runTest {
        // Arrange
        val userWallet: UserWallet = mockk()
        every { walletDeviceLinkRepository.isLinked(userWallet) } returns flowOf(false, true)

        // Act
        val actual = useCase(userWallet).toList()

        // Assert
        assertThat(actual).containsExactly(false, true).inOrder()
    }
}