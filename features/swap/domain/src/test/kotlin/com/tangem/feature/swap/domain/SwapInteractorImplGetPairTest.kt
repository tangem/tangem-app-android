package com.tangem.feature.swap.domain

import arrow.core.left
import arrow.core.right
import com.google.common.truth.Truth.assertThat
import com.tangem.blockchain.common.Blockchain
import com.tangem.blockchainsdk.utils.toNetworkId
import com.tangem.domain.express.models.ExpressError
import com.tangem.domain.express.models.ExpressProviderType
import com.tangem.domain.swap.models.SwapTxType
import com.tangem.feature.swap.domain.models.domain.ExchangeProviderType
import io.mockk.coEvery
import io.mockk.coVerify
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class SwapInteractorImplGetPairTest : SwapInteractorImplTestBase() {

    private val ethNetwork = Blockchain.Ethereum.toNetworkId()
    private val btcNetwork = Blockchain.Bitcoin.toNetworkId()

    private val fromStatus = buildSwapCurrencyStatus(
        networkRawId = ethNetwork,
        contractAddress = "0",
        isCoin = true,
    )
    private val toStatus = buildSwapCurrencyStatus(
        networkRawId = btcNetwork,
        contractAddress = "0",
        isCoin = true,
    )

    @Nested
    inner class `getPair happy path` {

        @Test
        fun `should return Right with mapped SwapPairLeast list when use case succeeds`() = runTest {
            // Given
            val expressProvider = buildExpressProvider(providerId = "p1", type = ExpressProviderType.DEX)
            val pairModel = buildSwapPairModel(
                fromNetworkRawId = ethNetwork,
                fromContractAddress = "0",
                toNetworkRawId = btcNetwork,
                toContractAddress = "0",
                providers = listOf(expressProvider),
            )
            coEvery {
                getSwapPairUseCase.invoke(
                    primarySwapCurrencyStatus = fromStatus,
                    secondarySwapCurrencyStatus = toStatus,
                    filterProviderTypes = any(),
                    swapTxType = SwapTxType.Swap,
                )
            } returns listOf(pairModel).right()

            // When
            val result = sut.getPair(
                fromSwapCurrencyStatus = fromStatus,
                toSwapCurrencyStatus = toStatus,
                filterProviderTypes = listOf(ExchangeProviderType.DEX),
            )

            // Then
            assertThat(result.isRight()).isTrue()
            result.onRight { pairs ->
                assertThat(pairs).hasSize(1)
                val pair = pairs.first()
                assertThat(pair.from.network).isEqualTo(ethNetwork)
                assertThat(pair.from.contractAddress).isEqualTo("0")
                assertThat(pair.to.network).isEqualTo(btcNetwork)
                assertThat(pair.providers).hasSize(1)
                assertThat(pair.providers.first().providerId).isEqualTo("p1")
            }
        }

        @Test
        fun `should map coin contractAddress to 0 in LeastTokenInfo`() = runTest {
            // Given — coin currency (contractAddress = "0" by convention)
            val pairModel = buildSwapPairModel(
                fromNetworkRawId = ethNetwork,
                fromContractAddress = "0",
                toNetworkRawId = btcNetwork,
                toContractAddress = "0",
            )
            coEvery {
                getSwapPairUseCase.invoke(
                    primarySwapCurrencyStatus = any(),
                    secondarySwapCurrencyStatus = any(),
                    filterProviderTypes = any(),
                    swapTxType = any(),
                )
            } returns listOf(pairModel).right()

            // When
            val result = sut.getPair(fromStatus, toStatus, emptyList())

            // Then
            assertThat(result.isRight()).isTrue()
            result.onRight { pairs ->
                assertThat(pairs.first().from.contractAddress).isEqualTo("0")
                assertThat(pairs.first().to.contractAddress).isEqualTo("0")
            }
        }

        @Test
        fun `should map all ExchangeProviderType variants to ExpressProviderType correctly`() = runTest {
            // Given
            coEvery {
                getSwapPairUseCase.invoke(
                    primarySwapCurrencyStatus = any(),
                    secondarySwapCurrencyStatus = any(),
                    filterProviderTypes = listOf(
                        ExpressProviderType.DEX,
                        ExpressProviderType.CEX,
                        ExpressProviderType.DEX_BRIDGE,
                    ),
                    swapTxType = SwapTxType.Swap,
                )
            } returns emptyList<com.tangem.domain.swap.models.SwapPairModel>().right()

            // When
            val result = sut.getPair(
                fromSwapCurrencyStatus = fromStatus,
                toSwapCurrencyStatus = toStatus,
                filterProviderTypes = listOf(
                    ExchangeProviderType.DEX,
                    ExchangeProviderType.CEX,
                    ExchangeProviderType.DEX_BRIDGE,
                ),
            )

            // Then
            assertThat(result.isRight()).isTrue()
            coVerify(exactly = 1) {
                getSwapPairUseCase.invoke(
                    primarySwapCurrencyStatus = any(),
                    secondarySwapCurrencyStatus = any(),
                    filterProviderTypes = listOf(
                        ExpressProviderType.DEX,
                        ExpressProviderType.CEX,
                        ExpressProviderType.DEX_BRIDGE,
                    ),
                    swapTxType = SwapTxType.Swap,
                )
            }
        }

        @Test
        fun `should return empty list when use case returns empty pairs`() = runTest {
            // Given
            coEvery {
                getSwapPairUseCase.invoke(
                    primarySwapCurrencyStatus = any(),
                    secondarySwapCurrencyStatus = any(),
                    filterProviderTypes = any(),
                    swapTxType = any(),
                )
            } returns emptyList<com.tangem.domain.swap.models.SwapPairModel>().right()

            // When
            val result = sut.getPair(fromStatus, toStatus, emptyList())

            // Then
            assertThat(result.isRight()).isTrue()
            result.onRight { pairs ->
                assertThat(pairs).isEmpty()
            }
        }
    }

    @Nested
    inner class `getPair error path` {

        @Test
        fun `should return Left with ExpressError when use case returns Left`() = runTest {
            // Given
            val expectedError = ExpressError.DataError(code = 400, description = "bad request")
            coEvery {
                getSwapPairUseCase.invoke(
                    primarySwapCurrencyStatus = any(),
                    secondarySwapCurrencyStatus = any(),
                    filterProviderTypes = any(),
                    swapTxType = any(),
                )
            } returns expectedError.left()

            // When
            val result = sut.getPair(fromStatus, toStatus, emptyList())

            // Then
            assertThat(result.isLeft()).isTrue()
            result.onLeft { error ->
                assertThat(error).isInstanceOf(ExpressError.DataError::class.java)
                assertThat((error as ExpressError.DataError).code).isEqualTo(400)
            }
        }
    }
}