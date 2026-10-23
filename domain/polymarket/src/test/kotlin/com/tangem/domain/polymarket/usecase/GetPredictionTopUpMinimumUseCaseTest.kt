package com.tangem.domain.polymarket.usecase

import com.google.common.truth.Truth.assertThat
import com.tangem.blockchain.common.Blockchain
import com.tangem.blockchainsdk.utils.toNetworkId
import com.tangem.domain.models.currency.CryptoCurrency
import com.tangem.domain.models.wallet.UserWalletId
import com.tangem.domain.polymarket.PredictionDepositRequisitesStore
import com.tangem.domain.polymarket.approval.PolymarketContracts
import com.tangem.domain.polymarket.model.PolymarketChainFamily
import com.tangem.domain.polymarket.model.PolymarketDepositNetwork
import com.tangem.domain.polymarket.model.PolymarketDepositRequisites
import com.tangem.domain.polymarket.model.PolymarketDepositToken
import io.mockk.clearMocks
import io.mockk.coEvery
import io.mockk.every
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
        coEvery { requisitesStore.getSyncOrNull(USER_WALLET_ID) } returns PolymarketDepositRequisites(
            networks = listOf(
                network(chainId = POLYGON_CHAIN_ID, tokenAddress = POLYGON_USDC, minCheckoutUsd = BigDecimal("2")),
                network(chainId = ETHEREUM_CHAIN_ID, tokenAddress = ETHEREUM_USDC, minCheckoutUsd = BigDecimal("7")),
            ),
        )
    }

    @Test
    fun `GIVEN Polygon USDC WHEN invoke THEN returns the Polygon minimum`() = runTest {
        // Act
        val actual = useCase(USER_WALLET_ID, token(Blockchain.Polygon, POLYGON_USDC))

        // Assert
        assertThat(actual).isEqualTo(BigDecimal("2"))
    }

    @Test
    fun `GIVEN Ethereum USDC in checksum case WHEN invoke THEN returns the Ethereum minimum`() = runTest {
        // Act
        val actual = useCase(USER_WALLET_ID, token(Blockchain.Ethereum, ETHEREUM_USDC_CHECKSUM))

        // Assert
        assertThat(actual).isEqualTo(BigDecimal("7"))
    }

    @Test
    fun `GIVEN a token the bridge lists no minimum for WHEN invoke THEN returns null`() = runTest {
        // Arrange
        coEvery { requisitesStore.getSyncOrNull(USER_WALLET_ID) } returns PolymarketDepositRequisites(
            networks = listOf(network(chainId = POLYGON_CHAIN_ID, tokenAddress = POLYGON_USDC, minCheckoutUsd = null)),
        )

        // Act
        val actual = useCase(USER_WALLET_ID, token(Blockchain.Polygon, POLYGON_USDC))

        // Assert
        assertThat(actual).isNull()
    }

    @Test
    fun `GIVEN a token that is not a catalogued deposit asset WHEN invoke THEN returns null`() = runTest {
        // Act
        val actual = useCase(USER_WALLET_ID, token(Blockchain.Polygon, "0xc2132d05d31c914a87c6611c10748aeb04b58e8f"))

        // Assert
        assertThat(actual).isNull()
    }

    @Test
    fun `GIVEN nothing is stored WHEN invoke THEN returns null`() = runTest {
        // Arrange
        coEvery { requisitesStore.getSyncOrNull(USER_WALLET_ID) } returns null

        // Act
        val actual = useCase(USER_WALLET_ID, token(Blockchain.Polygon, POLYGON_USDC))

        // Assert
        assertThat(actual).isNull()
    }

    private fun network(chainId: String, tokenAddress: String, minCheckoutUsd: BigDecimal?) = PolymarketDepositNetwork(
        chainId = chainId,
        chainName = chainId,
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
    )

    private fun token(blockchain: Blockchain, contractAddress: String): CryptoCurrency.Token = mockk {
        every { network.rawId } returns blockchain.toNetworkId()
        every { this@mockk.contractAddress } returns contractAddress
    }

    private companion object {
        val USER_WALLET_ID = UserWalletId("011")
        val POLYGON_CHAIN_ID = PolymarketContracts.CHAIN_ID.toString()
        const val ETHEREUM_CHAIN_ID = "1"
        const val POLYGON_USDC = PolymarketContracts.USDC_TOKEN_ADDRESS
        const val ETHEREUM_USDC = "0xa0b86991c6218b36c1d19d4a2e9eb0ce3606eb48"
        const val ETHEREUM_USDC_CHECKSUM = "0xA0b86991c6218b36c1d19D4a2e9Eb0cE3606eB48"
    }
}