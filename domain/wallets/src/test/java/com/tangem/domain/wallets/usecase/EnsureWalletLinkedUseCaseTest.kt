package com.tangem.domain.wallets.usecase

import com.google.common.truth.Truth.assertThat
import com.tangem.domain.models.wallet.UserWallet
import com.tangem.domain.wallets.registration.WalletDeviceLinkRepository
import com.tangem.domain.wallets.registration.WalletLinkRequester
import com.tangem.test.core.ProvideTestModels
import io.mockk.clearMocks
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.params.ParameterizedTest

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class EnsureWalletLinkedUseCaseTest {

    private val walletDeviceLinkRepository: WalletDeviceLinkRepository = mockk()
    private val walletLinkRequester: WalletLinkRequester = mockk()
    private val userWallet: UserWallet = mockk()

    private val useCase = EnsureWalletLinkedUseCase(walletDeviceLinkRepository, walletLinkRequester)

    @BeforeEach
    fun setup() {
        clearMocks(walletDeviceLinkRepository, walletLinkRequester)
    }

    @Test
    fun `GIVEN linked wallet WHEN invoke THEN returns true without asking the user`() = runTest {
        // Arrange
        every { walletDeviceLinkRepository.isLinked(userWallet) } returns flowOf(true)

        // Act
        val actual = useCase(userWallet)

        // Assert
        assertThat(actual).isTrue()
        coVerify(exactly = 0) { walletLinkRequester.requestLink(any()) }
    }

    @Test
    fun `GIVEN link status unknown WHEN invoke THEN returns true without asking the user`() = runTest {
        // Arrange
        every { walletDeviceLinkRepository.isLinked(userWallet) } returns emptyFlow()

        // Act
        val actual = useCase(userWallet)

        // Assert
        assertThat(actual).isTrue()
        coVerify(exactly = 0) { walletLinkRequester.requestLink(any()) }
    }

    @ParameterizedTest
    @ProvideTestModels
    fun `GIVEN not linked wallet WHEN invoke THEN asks the user and returns whether it got linked`(
        model: RequestModel,
    ) = runTest {
        // Arrange
        every { walletDeviceLinkRepository.isLinked(userWallet) } returns flowOf(false)
        coEvery { walletLinkRequester.requestLink(userWallet) } returns model.result

        // Act
        val actual = useCase(userWallet)

        // Assert
        assertThat(actual).isEqualTo(model.expected)
        coVerify(exactly = 1) { walletLinkRequester.requestLink(userWallet) }
    }

    private fun provideTestModels() = listOf(
        RequestModel(result = WalletLinkRequester.Result.Linked, expected = true),
        RequestModel(result = WalletLinkRequester.Result.Dismissed, expected = false),
    )

    data class RequestModel(val result: WalletLinkRequester.Result, val expected: Boolean)
}