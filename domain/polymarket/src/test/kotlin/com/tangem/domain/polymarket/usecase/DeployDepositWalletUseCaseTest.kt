package com.tangem.domain.polymarket.usecase

import arrow.core.left
import arrow.core.right
import com.google.common.truth.Truth.assertThat
import com.tangem.domain.models.wallet.UserWalletId
import com.tangem.domain.polymarket.PolymarketRepository
import com.tangem.domain.polymarket.model.PolymarketAddresses
import com.tangem.domain.polymarket.model.PolymarketOnboardingError
import com.tangem.domain.polymarket.model.PolymarketWalletError
import com.tangem.domain.polymarket.model.PolymarketWalletStatus
import io.mockk.clearMocks
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

internal class DeployDepositWalletUseCaseTest {

    private val polymarketRepository: PolymarketRepository = mockk()

    private val useCase = DeployDepositWalletUseCase(polymarketRepository = polymarketRepository)

    private val userWalletId = UserWalletId("011")
    private val addresses = PolymarketAddresses(
        ownerAddress = OWNER,
        depositWalletAddress = DEPOSIT_WALLET,
        userWalletId = userWalletId,
    )

    @BeforeEach
    fun resetMocks() {
        clearMocks(polymarketRepository)
    }

    @Test
    fun `GIVEN derived addresses WHEN invoke THEN deploys the locally derived wallet`() = runTest {
        // Arrange
        coEvery {
            polymarketRepository.deployWallet(OWNER, userWalletId, DEPOSIT_WALLET)
        } returns PolymarketWalletStatus.DEPLOYMENT_IN_PROGRESS.right()

        // Act
        val actual = useCase(addresses = addresses)

        // Assert
        assertThat(actual).isEqualTo(PolymarketWalletStatus.DEPLOYMENT_IN_PROGRESS.right())
        coVerify(exactly = 1) { polymarketRepository.deployWallet(OWNER, userWalletId, DEPOSIT_WALLET) }
    }

    @Test
    fun `GIVEN the relayer is unavailable WHEN invoke THEN wraps the wallet error`() = runTest {
        // Arrange
        coEvery {
            polymarketRepository.deployWallet(OWNER, userWalletId, DEPOSIT_WALLET)
        } returns PolymarketWalletError.RelayerUnavailable.left()

        // Act
        val actual = useCase(addresses = addresses)

        // Assert
        assertThat(actual).isEqualTo(
            PolymarketOnboardingError.Wallet(PolymarketWalletError.RelayerUnavailable).left(),
        )
    }

    @Test
    fun `GIVEN there is no connection WHEN invoke THEN returns Network`() = runTest {
        // Arrange
        coEvery {
            polymarketRepository.deployWallet(OWNER, userWalletId, DEPOSIT_WALLET)
        } returns PolymarketWalletError.Network.left()

        // Act
        val actual = useCase(addresses = addresses)

        // Assert
        assertThat(actual).isEqualTo(PolymarketOnboardingError.Network.left())
    }

    private companion object {
        const val OWNER = "0x1111111111111111111111111111111111111111"
        const val DEPOSIT_WALLET = "0xfAeA0f08159fcF2f573fE24E9E989B0d48f7651B"
    }
}