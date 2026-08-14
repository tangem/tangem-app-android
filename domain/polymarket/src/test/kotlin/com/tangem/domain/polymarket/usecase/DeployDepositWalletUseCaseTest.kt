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

    private val addresses = PolymarketAddresses(
        ownerAddress = OWNER,
        depositWalletAddress = DEPOSIT_WALLET,
        userWalletId = UserWalletId(TANGEM_WALLET_ID),
    )

    @BeforeEach
    fun resetMocks() {
        clearMocks(polymarketRepository)
    }

    /**
     * Pinned to the BFF's own swagger example, not to a formula of ours: `walletId` travels as the Tangem
     * wallet id exactly as stored — unprefixed, upper-case. The field has been misread twice by inferring a
     * shape instead of copying the contract's, so any transformation added here needs the contract to say so.
     */
    @Test
    fun `GIVEN derived addresses WHEN invoke THEN the Tangem wallet id is sent verbatim`() = runTest {
        // Arrange
        coEvery {
            polymarketRepository.deployWallet(OWNER, TANGEM_WALLET_ID, DEPOSIT_WALLET)
        } returns PolymarketWalletStatus.DEPLOYMENT_IN_PROGRESS.right()

        // Act
        val actual = useCase(addresses = addresses)

        // Assert
        assertThat(actual).isEqualTo(PolymarketWalletStatus.DEPLOYMENT_IN_PROGRESS.right())
        coVerify(exactly = 1) { polymarketRepository.deployWallet(OWNER, TANGEM_WALLET_ID, DEPOSIT_WALLET) }
    }

    /**
     * The pass-through test above proves the value survives untouched; it cannot prove the value was the
     * right shape to begin with, because it builds the id from the contract's own example. This pins the
     * other half — a wallet id really is unprefixed upper-case 64-hex, so nothing here owes it a conversion.
     * `UserWalletIdBuilder` produces it through `calculateHmacSha256` (32 bytes) and `ByteArray.toHexString()`.
     */
    @Test
    fun `GIVEN a wallet id built from bytes WHEN read THEN it is unprefixed upper-case 64-hex`() {
        // Act
        val actual = UserWalletId(value = ByteArray(size = 32) { 0xAB.toByte() }).stringValue

        // Assert
        assertThat(actual).matches("[0-9A-F]{64}")
    }

    @Test
    fun `GIVEN the relayer is unavailable WHEN invoke THEN wraps the wallet error`() = runTest {
        // Arrange
        coEvery {
            polymarketRepository.deployWallet(OWNER, TANGEM_WALLET_ID, DEPOSIT_WALLET)
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
            polymarketRepository.deployWallet(OWNER, TANGEM_WALLET_ID, DEPOSIT_WALLET)
        } returns PolymarketWalletError.Network.left()

        // Act
        val actual = useCase(addresses = addresses)

        // Assert
        assertThat(actual).isEqualTo(PolymarketOnboardingError.Network.left())
    }

    private companion object {
        const val OWNER = "0x1111111111111111111111111111111111111111"
        const val TANGEM_WALLET_ID = "7CE25DC32EF792CFC32380007A4172F5B64F67E4F91D37F14B351A76DAFA33DA"
        const val DEPOSIT_WALLET = "0xfAeA0f08159fcF2f573fE24E9E989B0d48f7651B"
    }
}