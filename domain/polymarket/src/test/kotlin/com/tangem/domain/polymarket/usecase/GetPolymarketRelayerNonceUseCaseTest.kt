package com.tangem.domain.polymarket.usecase

import arrow.core.left
import arrow.core.right
import com.google.common.truth.Truth.assertThat
import com.tangem.domain.core.error.DataError
import com.tangem.domain.models.wallet.UserWalletId
import com.tangem.domain.polymarket.PolymarketRepository
import com.tangem.domain.polymarket.model.PolymarketAddresses
import com.tangem.domain.polymarket.model.PolymarketOnboardingError
import io.mockk.clearMocks
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.math.BigInteger

internal class GetPolymarketRelayerNonceUseCaseTest {

    private val polymarketRepository: PolymarketRepository = mockk()

    private val useCase = GetPolymarketRelayerNonceUseCase(polymarketRepository = polymarketRepository)

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
    fun `GIVEN the relayer returns a nonce WHEN invoke THEN returns it`() = runTest {
        // Arrange
        coEvery { polymarketRepository.getRelayerNonce(OWNER) } returns BigInteger.ZERO.right()

        // Act
        val actual = useCase(addresses = addresses)

        // Assert
        assertThat(actual).isEqualTo(BigInteger.ZERO.right())
    }

    @Test
    fun `GIVEN an unexpected data error WHEN invoke THEN returns Unknown`() = runTest {
        // Arrange
        coEvery { polymarketRepository.getRelayerNonce(OWNER) } returns
            DataError.UserWalletError.WrongUserWallet(message = "boom").left()

        // Act
        val actual = useCase(addresses = addresses)

        // Assert
        assertThat(actual).isEqualTo(PolymarketOnboardingError.Unknown.left())
    }

    private companion object {
        const val OWNER = "0x7E5F4552091A69125d5DfCb7b8C2659029395Bdf"
        const val DEPOSIT_WALLET = "0xfAeA0f08159fcF2f573fE24E9E989B0d48f7651B"
    }
}