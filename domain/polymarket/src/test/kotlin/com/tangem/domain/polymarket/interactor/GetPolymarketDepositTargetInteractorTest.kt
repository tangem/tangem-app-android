package com.tangem.domain.polymarket.interactor

import arrow.core.Either
import com.google.common.truth.Truth.assertThat
import com.tangem.domain.models.wallet.UserWalletId
import com.tangem.domain.polymarket.PolymarketBridgeRepository
import com.tangem.domain.polymarket.model.PolymarketAddresses
import com.tangem.domain.polymarket.model.PolymarketBridgeAddresses
import com.tangem.domain.polymarket.model.PolymarketBridgeAsset
import com.tangem.domain.polymarket.model.PolymarketBridgeError
import com.tangem.domain.polymarket.model.PolymarketDepositTarget
import com.tangem.domain.polymarket.usecase.DerivePolymarketAddressesUseCase
import io.mockk.clearMocks
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.math.BigDecimal

internal class GetPolymarketDepositTargetInteractorTest {

    private val bridgeRepository: PolymarketBridgeRepository = mockk()
    private val deriveAddresses: DerivePolymarketAddressesUseCase = mockk()

    private val interactor = GetPolymarketDepositTargetInteractor(
        bridgeRepository = bridgeRepository,
        deriveAddresses = deriveAddresses,
    )

    private val userWalletId = UserWalletId(stringValue = WALLET_ID)

    private val bridgeAddresses = PolymarketBridgeAddresses(
        evm = BRIDGE_ADDRESS,
        svm = "sol1",
        btc = "bc1",
        tron = null,
    )

    private val polygonUsdc = PolymarketBridgeAsset(
        chainId = 137,
        chainName = "Polygon",
        tokenSymbol = "USDC",
        tokenAddress = "0x3c49",
        decimals = 6,
        minCheckoutUsd = BigDecimal("2"),
    )

    @BeforeEach
    fun resetMocks() {
        clearMocks(bridgeRepository, deriveAddresses)
        coEvery { deriveAddresses.stored(userWalletId) } returns addresses()
    }

    @Test
    fun `GIVEN polygon usdc is accepted WHEN invoked THEN the address and its minimum come back`() = runTest {
        // Arrange
        coEvery { bridgeRepository.getSupportedAssets() } returns Either.Right(listOf(polygonUsdc))
        coEvery { bridgeRepository.createDepositAddresses(any(), any()) } returns Either.Right(bridgeAddresses)

        // Act
        val actual = interactor(userWalletId)

        // Assert
        assertThat(actual).isEqualTo(
            Either.Right(PolymarketDepositTarget(addresses = bridgeAddresses, asset = polygonUsdc)),
        )
        coVerify(exactly = 1) { bridgeRepository.createDepositAddresses(WALLET_ID, DEPOSIT_WALLET) }
    }

    @Test
    fun `GIVEN polygon usdc is not accepted WHEN invoked THEN no address is issued`() = runTest {
        // Arrange
        coEvery { bridgeRepository.getSupportedAssets() } returns Either.Right(
            listOf(polygonUsdc.copy(chainId = 8453, chainName = "Base")),
        )

        // Act
        val actual = interactor(userWalletId)

        // Assert
        assertThat(actual).isEqualTo(Either.Left(PolymarketBridgeError.Unavailable))
        coVerify(exactly = 0) { bridgeRepository.createDepositAddresses(any(), any()) }
    }

    @Test
    fun `GIVEN the accepted list cannot be read WHEN invoked THEN no address is issued`() = runTest {
        // Arrange
        coEvery { bridgeRepository.getSupportedAssets() } returns Either.Left(PolymarketBridgeError.Network)

        // Act
        val actual = interactor(userWalletId)

        // Assert
        assertThat(actual).isEqualTo(Either.Left(PolymarketBridgeError.Network))
        coVerify(exactly = 0) { bridgeRepository.createDepositAddresses(any(), any()) }
    }

    @Test
    fun `GIVEN the deposit wallet is unknown here WHEN invoked THEN nothing is asked of the user`() = runTest {
        // Arrange
        coEvery { bridgeRepository.getSupportedAssets() } returns Either.Right(listOf(polygonUsdc))
        coEvery { deriveAddresses.stored(userWalletId) } returns null

        // Act
        val actual = interactor(userWalletId)

        // Assert
        assertThat(actual).isEqualTo(Either.Left(PolymarketBridgeError.Unavailable))
        coVerify(exactly = 0) { deriveAddresses.invoke(any()) }
        coVerify(exactly = 0) { bridgeRepository.createDepositAddresses(any(), any()) }
    }

    private fun addresses(): PolymarketAddresses = mockk {
        every { depositWalletAddress } returns DEPOSIT_WALLET
    }

    private companion object {
        const val WALLET_ID = "7CE25DC32EF792CFC32380007A4172F5B64F67E4F91D37F14B351A76DAFA33DA"
        const val DEPOSIT_WALLET = "0xDEf0000000000000000000000000000000000002"
        const val BRIDGE_ADDRESS = "0x5f3b000000000000000000000000000000000003"
    }
}