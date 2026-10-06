package com.tangem.domain.polymarket.support

import com.google.common.truth.Truth.assertThat
import com.tangem.domain.models.StatusSource
import com.tangem.domain.models.account.PredictionAccountStatusValue
import com.tangem.domain.models.currency.CryptoCurrency
import com.tangem.domain.models.wallet.UserWalletId
import com.tangem.domain.polymarket.flow.PredictionAccountStatusSupplier
import com.tangem.domain.polymarket.flow.PredictionPositionsValueSupplier
import com.tangem.domain.polymarket.model.PolymarketAddresses
import com.tangem.domain.polymarket.model.PredictionPositionsValue
import com.tangem.domain.polymarket.usecase.DerivePolymarketAddressesUseCase
import io.mockk.clearMocks
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.math.BigDecimal

internal class PredictionSupportChatInfoProviderTest {

    private val statusSupplier: PredictionAccountStatusSupplier = mockk()
    private val positionsSupplier: PredictionPositionsValueSupplier = mockk()
    private val deriveAddresses: DerivePolymarketAddressesUseCase = mockk()

    private val provider = PredictionSupportChatInfoProvider(
        predictionAccountStatusSupplier = statusSupplier,
        predictionPositionsValueSupplier = positionsSupplier,
        derivePolymarketAddressesUseCase = deriveAddresses,
    )

    @BeforeEach
    fun setUp() {
        clearMocks(statusSupplier, positionsSupplier, deriveAddresses)
        every { positionsSupplier(WALLET_ID) } returns flowOf(PredictionPositionsValue.Unknown)
        coEvery { deriveAddresses.stored(WALLET_ID) } returns ADDRESSES
    }

    @Test
    fun `GIVEN an active account with known positions WHEN getInfo THEN reports state, addresses and balances`() =
        runTest {
            // Arrange
            every { statusSupplier(WALLET_ID) } returns flowOf(activeStatus(depositAddress = BRIDGE_ADDRESS))
            every { positionsSupplier(WALLET_ID) } returns flowOf(
                PredictionPositionsValue.Known(amount = BigDecimal("7.5"), source = StatusSource.ACTUAL),
            )

            // Act
            val actual = provider.getInfo(WALLET_ID)

            // Assert
            assertThat(actual).isEqualTo(
                """
                Polymarket account state: active, source ACTUAL
                Polymarket deposit wallet address: $DEPOSIT_WALLET
                Polymarket available balance: 12.5
                Polymarket total balance: 20.0, source ACTUAL
                Polymarket bridge deposit address: $BRIDGE_ADDRESS
                """.trimIndent(),
            )
        }

    @Test
    fun `GIVEN an active account with unknown positions WHEN getInfo THEN the total is reported as unknown`() =
        runTest {
            // Arrange
            every { statusSupplier(WALLET_ID) } returns flowOf(activeStatus(depositAddress = null))

            // Act
            val actual = provider.getInfo(WALLET_ID)

            // Assert
            assertThat(actual).contains("Polymarket total balance: [positions value unknown]")
            assertThat(actual).contains("Polymarket bridge deposit address: [not loaded]")
        }

    @Test
    fun `GIVEN no account and no stored owner WHEN getInfo THEN reports not onboarded without balances`() = runTest {
        // Arrange
        every { statusSupplier(WALLET_ID) } returns flowOf(PredictionAccountStatusValue.NotOnboarded)
        coEvery { deriveAddresses.stored(WALLET_ID) } returns null

        // Act
        val actual = provider.getInfo(WALLET_ID)

        // Assert
        assertThat(actual).isEqualTo(
            """
            Polymarket account state: not onboarded, source ACTUAL
            Polymarket deposit wallet address: [not derived on this device]
            Polymarket available balance: [not loaded]
            Polymarket total balance: [not loaded]
            """.trimIndent(),
        )
    }

    @Test
    fun `GIVEN onboarding in progress WHEN getInfo THEN reports the stage`() = runTest {
        // Arrange
        every { statusSupplier(WALLET_ID) } returns flowOf(
            PredictionAccountStatusValue.Onboarding(
                source = StatusSource.CACHE,
                stage = PredictionAccountStatusValue.Onboarding.Stage.DEPLOYED,
            ),
        )

        // Act
        val actual = provider.getInfo(WALLET_ID)

        // Assert
        assertThat(actual).contains("Polymarket account state: onboarding (DEPLOYED), source CACHE")
    }

    @Test
    fun `GIVEN the status never emits WHEN getInfo THEN the state is reported as not loaded`() = runTest {
        // Arrange
        every { statusSupplier(WALLET_ID) } returns emptyFlow()
        every { positionsSupplier(WALLET_ID) } returns emptyFlow()

        // Act
        val actual = provider.getInfo(WALLET_ID)

        // Assert
        assertThat(actual).contains("Polymarket account state: [not loaded]")
    }

    private fun activeStatus(depositAddress: String?) = PredictionAccountStatusValue.Active(
        source = StatusSource.ACTUAL,
        balance = BigDecimal("12.5"),
        fiatRate = BigDecimal.ONE,
        depositCurrency = COLLATERAL,
        depositAddress = depositAddress,
    )

    private companion object {
        val WALLET_ID = UserWalletId("011")
        const val DEPOSIT_WALLET = "0xC0FFEE0000000000000000000000000000C0FFEE"
        const val BRIDGE_ADDRESS = "0xB41D6E0000000000000000000000000000B41D6E"
        val COLLATERAL: CryptoCurrency.Token = mockk(relaxed = true)
        val ADDRESSES = PolymarketAddresses(
            ownerAddress = "0x0000000000000000000000000000000000000001",
            depositWalletAddress = DEPOSIT_WALLET,
            userWalletId = WALLET_ID,
        )
    }
}