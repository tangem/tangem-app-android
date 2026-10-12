package com.tangem.domain.polymarket.usecase

import com.google.common.truth.Truth.assertThat
import com.tangem.domain.models.wallet.UserWalletId
import com.tangem.domain.polymarket.PredictionDepositRequisitesStore
import com.tangem.domain.polymarket.approval.PolymarketContracts
import com.tangem.domain.polymarket.model.PolymarketChainFamily
import com.tangem.domain.polymarket.model.PolymarketDepositNetwork
import com.tangem.domain.polymarket.model.PolymarketDepositRequisites
import com.tangem.domain.polymarket.model.PolymarketDepositToken
import io.mockk.clearMocks
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.math.BigDecimal

internal class GetPredictionTopUpMinimumUseCaseTest {

    private val requisitesStore: PredictionDepositRequisitesStore = mockk()

    private val useCase = GetPredictionTopUpMinimumUseCase(requisitesStore = requisitesStore)

    @BeforeEach
    fun resetMocks() {
        clearMocks(requisitesStore)
    }

    @Test
    fun `GIVEN default token has a minimum WHEN invoke THEN returns it`() = runTest {
        // Arrange
        coEvery { requisitesStore.getSyncOrNull(USER_WALLET_ID) } returns requisites(
            chainId = POLYGON_CHAIN_ID,
            tokenAddress = PolymarketContracts.USDC_TOKEN_ADDRESS,
            minCheckoutUsd = BigDecimal("2"),
        )

        // Act
        val actual = useCase(userWalletId = USER_WALLET_ID)

        // Assert
        assertThat(actual).isEqualTo(BigDecimal("2"))
    }

    @Test
    fun `GIVEN default token has no minimum WHEN invoke THEN returns null`() = runTest {
        // Arrange
        coEvery { requisitesStore.getSyncOrNull(USER_WALLET_ID) } returns requisites(
            chainId = POLYGON_CHAIN_ID,
            tokenAddress = PolymarketContracts.USDC_TOKEN_ADDRESS,
            minCheckoutUsd = null,
        )

        // Act
        val actual = useCase(userWalletId = USER_WALLET_ID)

        // Assert
        assertThat(actual).isNull()
    }

    @Test
    fun `GIVEN requisites have no default target WHEN invoke THEN returns null`() = runTest {
        // Arrange
        coEvery { requisitesStore.getSyncOrNull(USER_WALLET_ID) } returns requisites(
            chainId = "1",
            tokenAddress = PolymarketContracts.USDC_TOKEN_ADDRESS,
            minCheckoutUsd = BigDecimal("2"),
        )

        // Act
        val actual = useCase(userWalletId = USER_WALLET_ID)

        // Assert
        assertThat(actual).isNull()
    }

    @Test
    fun `GIVEN nothing is stored WHEN invoke THEN returns null`() = runTest {
        // Arrange
        coEvery { requisitesStore.getSyncOrNull(USER_WALLET_ID) } returns null

        // Act
        val actual = useCase(userWalletId = USER_WALLET_ID)

        // Assert
        assertThat(actual).isNull()
    }

    private fun requisites(chainId: String, tokenAddress: String, minCheckoutUsd: BigDecimal?) =
        PolymarketDepositRequisites(
            networks = listOf(
                PolymarketDepositNetwork(
                    chainId = chainId,
                    chainName = "Polygon",
                    family = PolymarketChainFamily.EVM,
                    address = "0xdeposit",
                    tokens = listOf(
                        PolymarketDepositToken(
                            symbol = "USDC",
                            address = tokenAddress,
                            decimals = 6,
                            minCheckoutUsd = minCheckoutUsd,
                        ),
                    ),
                ),
            ),
        )

    private companion object {
        val USER_WALLET_ID = UserWalletId("011")
        val POLYGON_CHAIN_ID = PolymarketContracts.CHAIN_ID.toString()
    }
}