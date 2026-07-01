package com.tangem.feature.swap.domain

import arrow.core.right
import com.google.common.truth.Truth.assertThat
import com.tangem.blockchain.common.Blockchain
import com.tangem.blockchainsdk.utils.toNetworkId
import com.tangem.domain.transaction.models.AssetRequirementsCondition
import com.tangem.feature.swap.domain.models.domain.ExchangeProviderType
import io.mockk.coEvery
import io.mockk.every
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance

/**
 * Tests for [SwapInteractorImpl.findProvidersForPair] and [SwapInteractorImpl.findProvidersForPairWithCheck].
 */
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class SwapInteractorImplFindProvidersForPairTest : SwapInteractorImplTestBase() {

    private val ethNetwork = Blockchain.Ethereum.toNetworkId()
    private val btcNetwork = Blockchain.Bitcoin.toNetworkId()

    @Nested
    inner class FindProvidersForPair {

        @Test
        fun `should return providers of the first pair whose to-contractAddress equals destination contract`() {
            // Given
            val fromStatus = buildSwapCurrencyStatus(networkRawId = ethNetwork, contractAddress = "0", isCoin = true)
            val toStatus = buildSwapCurrencyStatus(networkRawId = btcNetwork, contractAddress = "0", isCoin = true)
            val expectedProvider = buildSwapProvider(ExchangeProviderType.DEX, "expected")
            val matchingPair = buildSwapPairLeast(
                fromNetwork = ethNetwork,
                fromContract = "0",
                toNetwork = btcNetwork,
                toContract = "0",
                providers = listOf(expectedProvider),
            )

            // When
            val result = sut.findProvidersForPair(
                fromSwapCurrencyStatus = fromStatus,
                toSwapCurrencyStatus = toStatus,
                pairs = listOf(matchingPair),
            )

            // Then
            assertThat(result).containsExactly(expectedProvider)
        }

        @Test
        fun `should return empty list when pairs list is empty`() {
            // Given
            val fromStatus = buildSwapCurrencyStatus(networkRawId = ethNetwork)
            val toStatus = buildSwapCurrencyStatus(networkRawId = btcNetwork)

            // When
            val result = sut.findProvidersForPair(
                fromSwapCurrencyStatus = fromStatus,
                toSwapCurrencyStatus = toStatus,
                pairs = emptyList(),
            )

            // Then
            assertThat(result).isEmpty()
        }

        @Test
        fun `should return empty list when no pair's to-contractAddress equals destination contract`() {
            // Given — destination is a token with contractAddress "0xAbc", but pair's to-contract is "0"
            val fromStatus = buildSwapCurrencyStatus(networkRawId = ethNetwork, contractAddress = "0", isCoin = true)
            val toStatus = buildSwapCurrencyStatus(
                networkRawId = btcNetwork,
                contractAddress = "0xAbc",
                isCoin = false,
            )
            val unrelatedPair = buildSwapPairLeast(
                fromNetwork = ethNetwork,
                fromContract = "0",
                toNetwork = btcNetwork,
                toContract = "0",
                providers = listOf(buildSwapProvider(ExchangeProviderType.CEX, "unrelated")),
            )

            // When
            val result = sut.findProvidersForPair(
                fromSwapCurrencyStatus = fromStatus,
                toSwapCurrencyStatus = toStatus,
                pairs = listOf(unrelatedPair),
            )

            // Then
            assertThat(result).isEmpty()
        }
    }

    @Nested
    inner class FindProvidersForPairWithCheck {

        @Test
        fun `should return empty list when rampStateManager checkAssetRequirements returns false`() = runTest {
            // Given
            val fromStatus = buildSwapCurrencyStatus(networkRawId = ethNetwork)
            val toStatus = buildSwapCurrencyStatus(networkRawId = btcNetwork)
            val pair = buildSwapPairLeast(
                fromNetwork = ethNetwork,
                fromContract = "0",
                toNetwork = btcNetwork,
                toContract = "0",
            )

            coEvery {
                getAssetRequirementsUseCase.invoke(any(), any())
            } returns null.right()
            every { rampStateManager.checkAssetRequirements(any()) } returns false

            // When
            val result = sut.findProvidersForPairWithCheck(
                fromSwapCurrencyStatus = fromStatus,
                toSwapCurrencyStatus = toStatus,
                pairs = listOf(pair),
            )

            // Then
            assertThat(result).isEmpty()
        }

        @Test
        fun `should return providers from matching pair when checkAssetRequirements returns true`() = runTest {
            // Given
            val fromStatus = buildSwapCurrencyStatus(networkRawId = ethNetwork, contractAddress = "0", isCoin = true)
            val toStatus = buildSwapCurrencyStatus(networkRawId = btcNetwork, contractAddress = "0", isCoin = true)
            val providerA = buildSwapProvider(ExchangeProviderType.DEX, "A")
            val providerB = buildSwapProvider(ExchangeProviderType.CEX, "B")
            val pair = buildSwapPairLeast(
                fromNetwork = ethNetwork,
                fromContract = "0",
                toNetwork = btcNetwork,
                toContract = "0",
                providers = listOf(providerA, providerB),
            )

            coEvery {
                getAssetRequirementsUseCase.invoke(any(), any())
            } returns null.right()
            every { rampStateManager.checkAssetRequirements(any()) } returns true

            // When
            val result = sut.findProvidersForPairWithCheck(
                fromSwapCurrencyStatus = fromStatus,
                toSwapCurrencyStatus = toStatus,
                pairs = listOf(pair),
            )

            // Then
            assertThat(result).containsExactly(providerA, providerB)
        }

        @Test
        fun `should return empty list when destination asset requires association even if source is fulfilled`() =
            runTest {
                // Arrange — source has no requirements, but the destination (e.g. unassociated Hedera HTS token)
                // requires an on-chain opt-in. Without this check the swap would proceed and the payout would
                // get stuck (AND-Hedera ERC20/HTS association).
                val fromStatus = buildSwapCurrencyStatus(networkRawId = ethNetwork, contractAddress = "0", isCoin = true)
                val toStatus = buildSwapCurrencyStatus(
                    networkRawId = btcNetwork,
                    contractAddress = "0xAbc",
                    isCoin = false,
                )
                val pair = buildSwapPairLeast(
                    fromNetwork = ethNetwork,
                    fromContract = "0",
                    toNetwork = btcNetwork,
                    toContract = "0xAbc",
                    providers = listOf(buildSwapProvider(ExchangeProviderType.CEX, "A")),
                )

                val toRequirement = AssetRequirementsCondition.PaidTransaction
                coEvery {
                    getAssetRequirementsUseCase.invoke(any(), fromStatus.currency)
                } returns null.right()
                coEvery {
                    getAssetRequirementsUseCase.invoke(any(), toStatus.currency)
                } returns toRequirement.right()
                every { rampStateManager.checkAssetRequirements(null) } returns true
                every { rampStateManager.checkAssetRequirements(toRequirement) } returns false

                // Act
                val result = sut.findProvidersForPairWithCheck(
                    fromSwapCurrencyStatus = fromStatus,
                    toSwapCurrencyStatus = toStatus,
                    pairs = listOf(pair),
                )

                // Assert
                assertThat(result).isEmpty()
            }
    }

    @Nested
    inner class GetUnfulfilledReceiveRequirement {

        @Test
        fun `should return requirement when destination asset requirement is not fulfilled`() = runTest {
            // Arrange
            val toStatus = buildSwapCurrencyStatus(networkRawId = btcNetwork, contractAddress = "0xAbc", isCoin = false)
            val requirement = AssetRequirementsCondition.PaidTransaction
            coEvery { getAssetRequirementsUseCase.invoke(any(), any()) } returns requirement.right()
            every { rampStateManager.checkAssetRequirements(requirement) } returns false

            // Act
            val result = sut.getUnfulfilledReceiveRequirement(toStatus)

            // Assert
            assertThat(result).isEqualTo(requirement)
        }

        @Test
        fun `should return null when destination asset requirement is fulfilled`() = runTest {
            // Arrange
            val toStatus = buildSwapCurrencyStatus(networkRawId = btcNetwork, contractAddress = "0xAbc", isCoin = false)
            val requirement = AssetRequirementsCondition.PaidTransaction
            coEvery { getAssetRequirementsUseCase.invoke(any(), any()) } returns requirement.right()
            every { rampStateManager.checkAssetRequirements(requirement) } returns true

            // Act
            val result = sut.getUnfulfilledReceiveRequirement(toStatus)

            // Assert
            assertThat(result).isNull()
        }

        @Test
        fun `should return null when there is no destination asset requirement`() = runTest {
            // Arrange
            val toStatus = buildSwapCurrencyStatus(networkRawId = btcNetwork, contractAddress = "0xAbc", isCoin = false)
            coEvery { getAssetRequirementsUseCase.invoke(any(), any()) } returns null.right()
            every { rampStateManager.checkAssetRequirements(null) } returns true

            // Act
            val result = sut.getUnfulfilledReceiveRequirement(toStatus)

            // Assert
            assertThat(result).isNull()
        }
    }
}