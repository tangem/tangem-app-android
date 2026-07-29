package com.tangem.domain.polymarket.usecase

import arrow.core.left
import arrow.core.right
import com.google.common.truth.Truth.assertThat
import com.tangem.domain.models.wallet.UserWalletId
import com.tangem.domain.polymarket.PolymarketRepository
import com.tangem.domain.polymarket.model.PolymarketAddresses
import com.tangem.domain.polymarket.model.PolymarketOnboardingError
import com.tangem.domain.polymarket.model.PolymarketWalletError
import com.tangem.domain.polymarket.model.PolymarketWalletState
import com.tangem.domain.polymarket.model.PolymarketWalletStatus
import io.mockk.clearMocks
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

internal class GetPolymarketWalletStatusUseCaseTest {

    private val polymarketRepository: PolymarketRepository = mockk()

    private val useCase = GetPolymarketWalletStatusUseCase(polymarketRepository = polymarketRepository)

    private val addresses = PolymarketAddresses(
        ownerAddress = OWNER,
        depositWalletAddress = DEPOSIT_WALLET,
        userWalletId = UserWalletId("011"),
    )

    @BeforeEach
    fun resetMocks() {
        clearMocks(polymarketRepository)
    }

    @Test
    fun `GIVEN the backend knows no wallet yet WHEN invoke THEN returns the state`() = runTest {
        // Arrange
        val state = PolymarketWalletState(
            depositWalletAddress = null,
            status = PolymarketWalletStatus.NOT_CREATED,
        )
        coEvery { polymarketRepository.getWalletStatus(OWNER) } returns state.right()

        // Act
        val actual = useCase(addresses = addresses)

        // Assert
        assertThat(actual).isEqualTo(state.right())
    }

    @Test
    fun `GIVEN the backend returns the same address in another case WHEN invoke THEN returns the state`() = runTest {
        // Arrange
        val state = PolymarketWalletState(
            depositWalletAddress = DEPOSIT_WALLET.lowercase(),
            status = PolymarketWalletStatus.DEPLOYED,
        )
        coEvery { polymarketRepository.getWalletStatus(OWNER) } returns state.right()

        // Act
        val actual = useCase(addresses = addresses)

        // Assert
        assertThat(actual).isEqualTo(state.right())
    }

    @Test
    fun `GIVEN the backend returns another address WHEN invoke THEN returns AddressMismatch`() = runTest {
        // Arrange
        val state = PolymarketWalletState(
            depositWalletAddress = OTHER_DEPOSIT_WALLET,
            status = PolymarketWalletStatus.DEPLOYED,
        )
        coEvery { polymarketRepository.getWalletStatus(OWNER) } returns state.right()

        // Act
        val actual = useCase(addresses = addresses)

        // Assert
        val expected = PolymarketOnboardingError.AddressMismatch(
            expected = DEPOSIT_WALLET,
            actual = OTHER_DEPOSIT_WALLET,
        )
        assertThat(actual).isEqualTo(expected.left())
    }

    @Test
    fun `GIVEN the backend fails with a non-network error WHEN invoke THEN wraps the wallet error`() = runTest {
        // Arrange
        coEvery { polymarketRepository.getWalletStatus(OWNER) } returns PolymarketWalletError.Unauthorized.left()

        // Act
        val actual = useCase(addresses = addresses)

        // Assert
        assertThat(actual).isEqualTo(PolymarketOnboardingError.Wallet(PolymarketWalletError.Unauthorized).left())
    }

    @Test
    fun `GIVEN the backend fails with a network error WHEN invoke THEN returns Network`() = runTest {
        // Arrange
        coEvery { polymarketRepository.getWalletStatus(OWNER) } returns PolymarketWalletError.Network.left()

        // Act
        val actual = useCase(addresses = addresses)

        // Assert
        assertThat(actual).isEqualTo(PolymarketOnboardingError.Network.left())
    }

    private companion object {
        const val OWNER = "0x1111111111111111111111111111111111111111"
        const val DEPOSIT_WALLET = "0xfAeA0f08159fcF2f573fE24E9E989B0d48f7651B"
        const val OTHER_DEPOSIT_WALLET = "0x57ffBc34De23124fAeb8387fcd689d314E57aCcD"
    }
}