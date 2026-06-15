package com.tangem.feature.swap.domain

import com.google.common.truth.Truth.assertThat
import com.tangem.blockchain.common.Blockchain
import com.tangem.blockchainsdk.utils.toNetworkId
import com.tangem.feature.swap.domain.models.domain.ExchangeProviderType
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance

/**
 * Tests for [SwapInteractorImpl.extractFromSwapCurrencyFromPair].
 *
 * This function resolves which of the two [com.tangem.domain.swap.models.SwapCurrencyStatus]
 * arguments corresponds to the `from` side of a given [com.tangem.feature.swap.domain.models.domain.SwapPairLeast].
 *
 * It is the building block behind the Tangem Pay provider-filtering logic in `SwapModel`:
 * the resolved "from" currency status is inspected for an [com.tangem.domain.models.account.Account.Payment]
 * account; when it belongs to a payment account, only CEX providers are kept for that pair.
 *
 * A pair is matched on both `network` (rawId) and `contractAddress` ("0" for coins, the token
 * contract for tokens). The `from` side is checked first, then the `to` side, otherwise null.
 */
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class SwapInteractorImplExtractFromSwapCurrencyTest : SwapInteractorImplTestBase() {

    private val ethNetwork = Blockchain.Ethereum.toNetworkId()
    private val btcNetwork = Blockchain.Bitcoin.toNetworkId()
    private val polygonNetwork = Blockchain.Polygon.toNetworkId()

    @Nested
    inner class MatchesFromSide {

        @Test
        fun `should return fromSwapCurrencyStatus when pair from matches the from coin by network and contract`() {
            // Given — coin: getContractAddress() == "0"
            val fromStatus = buildSwapCurrencyStatus(networkRawId = ethNetwork, contractAddress = "0", isCoin = true)
            val toStatus = buildSwapCurrencyStatus(networkRawId = btcNetwork, contractAddress = "0", isCoin = true)
            val pair = buildSwapPairLeast(
                fromNetwork = ethNetwork,
                fromContract = "0",
                toNetwork = btcNetwork,
                toContract = "0",
            )

            // When
            val result = sut.extractFromSwapCurrencyFromPair(
                pair = pair,
                fromSwapCurrencyStatus = fromStatus,
                toSwapCurrencyStatus = toStatus,
            )

            // Then
            assertThat(result).isSameInstanceAs(fromStatus)
        }

        @Test
        fun `should return fromSwapCurrencyStatus when pair from matches the from token by network and contract`() {
            // Given — token: getContractAddress() == contractAddress
            val fromStatus = buildSwapCurrencyStatus(
                networkRawId = ethNetwork,
                contractAddress = "0xToken",
                isCoin = false,
            )
            val toStatus = buildSwapCurrencyStatus(networkRawId = btcNetwork, contractAddress = "0", isCoin = true)
            val pair = buildSwapPairLeast(
                fromNetwork = ethNetwork,
                fromContract = "0xToken",
                toNetwork = btcNetwork,
                toContract = "0",
            )

            // When
            val result = sut.extractFromSwapCurrencyFromPair(
                pair = pair,
                fromSwapCurrencyStatus = fromStatus,
                toSwapCurrencyStatus = toStatus,
            )

            // Then
            assertThat(result).isSameInstanceAs(fromStatus)
        }
    }

    @Nested
    inner class MatchesToSide {

        @Test
        fun `should return toSwapCurrencyStatus when pair from matches the to side (reverse-direction pair)`() {
            // Given — pair.from points at the toStatus currency, not the fromStatus
            val fromStatus = buildSwapCurrencyStatus(networkRawId = ethNetwork, contractAddress = "0", isCoin = true)
            val toStatus = buildSwapCurrencyStatus(networkRawId = btcNetwork, contractAddress = "0", isCoin = true)
            val pair = buildSwapPairLeast(
                fromNetwork = btcNetwork, // matches toStatus
                fromContract = "0",
                toNetwork = ethNetwork,
                toContract = "0",
            )

            // When
            val result = sut.extractFromSwapCurrencyFromPair(
                pair = pair,
                fromSwapCurrencyStatus = fromStatus,
                toSwapCurrencyStatus = toStatus,
            )

            // Then
            assertThat(result).isSameInstanceAs(toStatus)
        }

        @Test
        fun `should return toSwapCurrencyStatus when pair from matches to token by network and contract`() {
            // Given
            val fromStatus = buildSwapCurrencyStatus(networkRawId = ethNetwork, contractAddress = "0", isCoin = true)
            val toStatus = buildSwapCurrencyStatus(
                networkRawId = polygonNetwork,
                contractAddress = "0xUsdc",
                isCoin = false,
            )
            val pair = buildSwapPairLeast(
                fromNetwork = polygonNetwork, // matches toStatus token
                fromContract = "0xUsdc",
                toNetwork = ethNetwork,
                toContract = "0",
            )

            // When
            val result = sut.extractFromSwapCurrencyFromPair(
                pair = pair,
                fromSwapCurrencyStatus = fromStatus,
                toSwapCurrencyStatus = toStatus,
            )

            // Then
            assertThat(result).isSameInstanceAs(toStatus)
        }
    }

    @Nested
    inner class NoMatch {

        @Test
        fun `should return null when pair from matches neither from nor to`() {
            // Given — pair.from is on an unrelated network
            val fromStatus = buildSwapCurrencyStatus(networkRawId = ethNetwork, contractAddress = "0", isCoin = true)
            val toStatus = buildSwapCurrencyStatus(networkRawId = btcNetwork, contractAddress = "0", isCoin = true)
            val pair = buildSwapPairLeast(
                fromNetwork = polygonNetwork, // matches neither
                fromContract = "0",
                toNetwork = ethNetwork,
                toContract = "0",
            )

            // When
            val result = sut.extractFromSwapCurrencyFromPair(
                pair = pair,
                fromSwapCurrencyStatus = fromStatus,
                toSwapCurrencyStatus = toStatus,
            )

            // Then
            assertThat(result).isNull()
        }

        @Test
        fun `should return null when network matches but contract address differs`() {
            // Given — same eth network but different token contracts
            val fromStatus = buildSwapCurrencyStatus(
                networkRawId = ethNetwork,
                contractAddress = "0xAaa",
                isCoin = false,
            )
            val toStatus = buildSwapCurrencyStatus(
                networkRawId = ethNetwork,
                contractAddress = "0xBbb",
                isCoin = false,
            )
            val pair = buildSwapPairLeast(
                fromNetwork = ethNetwork,
                fromContract = "0xCcc", // matches neither contract
                toNetwork = ethNetwork,
                toContract = "0xAaa",
            )

            // When
            val result = sut.extractFromSwapCurrencyFromPair(
                pair = pair,
                fromSwapCurrencyStatus = fromStatus,
                toSwapCurrencyStatus = toStatus,
            )

            // Then
            assertThat(result).isNull()
        }

        @Test
        fun `should return null when contract matches but network differs`() {
            // Given — same contract address but on a different network than either status
            val fromStatus = buildSwapCurrencyStatus(
                networkRawId = ethNetwork,
                contractAddress = "0xShared",
                isCoin = false,
            )
            val toStatus = buildSwapCurrencyStatus(
                networkRawId = btcNetwork,
                contractAddress = "0",
                isCoin = true,
            )
            val pair = buildSwapPairLeast(
                fromNetwork = polygonNetwork, // contract matches fromStatus but network does not
                fromContract = "0xShared",
                toNetwork = ethNetwork,
                toContract = "0",
            )

            // When
            val result = sut.extractFromSwapCurrencyFromPair(
                pair = pair,
                fromSwapCurrencyStatus = fromStatus,
                toSwapCurrencyStatus = toStatus,
            )

            // Then
            assertThat(result).isNull()
        }
    }

    @Nested
    inner class Precedence {

        @Test
        fun `should prefer from side when both from and to would match the pair from`() {
            // Given — both statuses are the same network+contract; from must win (checked first)
            val fromStatus = buildSwapCurrencyStatus(networkRawId = ethNetwork, contractAddress = "0", isCoin = true)
            val toStatus = buildSwapCurrencyStatus(networkRawId = ethNetwork, contractAddress = "0", isCoin = true)
            val pair = buildSwapPairLeast(
                fromNetwork = ethNetwork,
                fromContract = "0",
                toNetwork = ethNetwork,
                toContract = "0",
            )

            // When
            val result = sut.extractFromSwapCurrencyFromPair(
                pair = pair,
                fromSwapCurrencyStatus = fromStatus,
                toSwapCurrencyStatus = toStatus,
            )

            // Then — from side has precedence and is returned, not the to side
            assertThat(result).isSameInstanceAs(fromStatus)
            assertThat(result).isNotSameInstanceAs(toStatus)
        }

        @Test
        fun `pair providers are irrelevant to the resolution`() {
            // Given — provider list should not affect which currency status is extracted
            val fromStatus = buildSwapCurrencyStatus(networkRawId = ethNetwork, contractAddress = "0", isCoin = true)
            val toStatus = buildSwapCurrencyStatus(networkRawId = btcNetwork, contractAddress = "0", isCoin = true)
            val pair = buildSwapPairLeast(
                fromNetwork = ethNetwork,
                fromContract = "0",
                toNetwork = btcNetwork,
                toContract = "0",
                providers = listOf(
                    buildSwapProvider(ExchangeProviderType.DEX, "dex"),
                    buildSwapProvider(ExchangeProviderType.CEX, "cex"),
                ),
            )

            // When
            val result = sut.extractFromSwapCurrencyFromPair(
                pair = pair,
                fromSwapCurrencyStatus = fromStatus,
                toSwapCurrencyStatus = toStatus,
            )

            // Then
            assertThat(result).isSameInstanceAs(fromStatus)
        }
    }
}